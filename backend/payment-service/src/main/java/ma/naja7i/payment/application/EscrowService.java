package ma.naja7i.payment.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.config.PaymentProperties;
import ma.naja7i.payment.domain.AuditOperation;
import ma.naja7i.payment.domain.AuditReferenceType;
import ma.naja7i.payment.domain.Escrow;
import ma.naja7i.payment.domain.EscrowStatus;
import ma.naja7i.payment.domain.OutboxEvent;
import ma.naja7i.payment.infrastructure.messaging.payload.EscrowLockedPayload;
import ma.naja7i.payment.infrastructure.messaging.payload.EscrowReleasedPayload;
import ma.naja7i.payment.infrastructure.persistence.EscrowRepository;
import ma.naja7i.payment.infrastructure.persistence.OutboxRepository;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the escrow lifecycle in response to booking events.
 *
 * <p>Every state change goes through {@link BalanceLedger#apply} so a row is
 * always written to {@code audit_log} atomically with the balance change.
 *
 * <p>Idempotency:
 * <ul>
 *   <li>{@link #onBookingRequested} — the unique constraint on
 *       {@code escrows.booking_id} prevents a duplicate lock. We also pre-check
 *       and log+skip cleanly.</li>
 *   <li>{@link #onBookingConfirmed}, {@link #onBookingCancelled},
 *       {@link #onDisputeResolved} — guarded by the current
 *       {@link EscrowStatus}. Replays on a non-LOCKED escrow are a no-op.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class EscrowService {

    private static final Logger log = LoggerFactory.getLogger(EscrowService.class);

    private final EscrowRepository escrowRepository;
    private final OutboxRepository outboxRepository;
    private final BalanceLedger balanceLedger;
    private final KafkaEventEnvelopeBuilder envelopeBuilder;
    private final PaymentProperties properties;

    /**
     * On {@code booking.requested}: verify the student has enough balance, lock
     * the funds (move them out of the wallet) into a new escrow row, and emit
     * {@code payment.escrow.locked.v1}.
     *
     * <p>If balance is insufficient: log + skip. The caller has already inserted
     * the {@code processed_events} row, so this event won't be retried. The
     * booking will be auto-cancelled by booking-service's slot TTL.
     */
    @Transactional
    public void onBookingRequested(UUID bookingId, UUID studentId, UUID profId,
                                   long amountMinor, UUID traceId) {
        if (amountMinor <= 0L) {
            log.warn("booking.requested with non-positive amount — skipping bookingId={}", bookingId);
            return;
        }
        if (escrowRepository.existsByBookingId(bookingId)) {
            // Idempotent: a previous run already locked the funds for this booking.
            log.debug("Escrow already exists for bookingId — skipping");
            return;
        }

        // Pre-check available balance (the BalanceLedger will also enforce it).
        long platformFeeMinor = computeFee(amountMinor);
        long profShareMinor = amountMinor - platformFeeMinor;
        UUID escrowId = UUID.randomUUID();

        try {
            balanceLedger.apply(studentId, -amountMinor,
                    AuditOperation.ESCROW_LOCK, AuditReferenceType.ESCROW, escrowId,
                    studentId, "Escrow lock for booking " + bookingId, traceId);
        } catch (BusinessException ex) {
            if (ex.code() == ErrorCode.PAYMENT_INSUFFICIENT_BALANCE) {
                // Caller's slot TTL job will handle cleanup.
                log.info("Insufficient balance for booking — no escrow created [trace={}]", traceId);
                return;
            }
            throw ex;
        }

        Escrow escrow = escrowRepository.saveAndFlush(Escrow.builder()
                .id(escrowId)
                .bookingId(bookingId)
                .studentId(studentId)
                .profId(profId)
                .amountMinor(amountMinor)
                .platformFeeMinor(platformFeeMinor)
                .profShareMinor(profShareMinor)
                .currency("MAD")
                .status(EscrowStatus.LOCKED)
                .build());

        enqueueEvent(escrow.getId(), bookingId.toString(), EventTopics.PAYMENT_ESCROW_LOCKED,
                new EscrowLockedPayload(escrow.getId(), bookingId, studentId, profId,
                        toMajor(amountMinor), amountMinor, platformFeeMinor, profShareMinor, "MAD"));
    }

    /**
     * On {@code booking.confirmed}: release the LOCKED escrow to the prof.
     * Credits {@code prof_share_minor} to the prof's wallet (platform keeps the fee).
     */
    @Transactional
    public void onBookingConfirmed(UUID bookingId, UUID traceId) {
        Escrow e = escrowRepository.findByBookingId(bookingId).orElse(null);
        if (e == null) {
            log.warn("booking.confirmed with no escrow — bookingId={}", bookingId);
            return;
        }
        if (e.getStatus() != EscrowStatus.LOCKED) {
            log.debug("Escrow not LOCKED ({}), skipping release", e.getStatus());
            return;
        }
        balanceLedger.apply(e.getProfId(), e.getProfShareMinor(),
                AuditOperation.ESCROW_RELEASE, AuditReferenceType.ESCROW, e.getId(),
                e.getProfId(), "Escrow released to PROF after booking confirm", traceId);

        e.setStatus(EscrowStatus.RELEASED_TO_PROF);
        e.setProfPayoutMinor(e.getProfShareMinor());
        e.setReleasedAt(Instant.now());
        escrowRepository.saveAndFlush(e);

        enqueueEvent(e.getId(), bookingId.toString(), EventTopics.PAYMENT_ESCROW_RELEASED,
                new EscrowReleasedPayload(e.getId(), bookingId, e.getStudentId(), e.getProfId(),
                        "PROF", e.getProfShareMinor(), null, "MAD"));
    }

    /**
     * On {@code booking.cancelled} OR {@code booking.declined}: refund the LOCKED
     * escrow back to the student (full amount, fee included).
     */
    @Transactional
    public void onBookingCancelled(UUID bookingId, UUID traceId, String reason) {
        Escrow e = escrowRepository.findByBookingId(bookingId).orElse(null);
        if (e == null) {
            log.debug("booking.cancelled with no escrow — bookingId={}", bookingId);
            return;
        }
        if (e.getStatus() != EscrowStatus.LOCKED) {
            log.debug("Escrow not LOCKED ({}), skipping refund", e.getStatus());
            return;
        }
        balanceLedger.apply(e.getStudentId(), e.getAmountMinor(),
                AuditOperation.REFUND, AuditReferenceType.ESCROW, e.getId(),
                e.getStudentId(),
                reason == null ? "Refund (booking cancelled)" : "Refund (" + reason + ")",
                traceId);

        e.setStatus(EscrowStatus.REFUNDED_TO_STUDENT);
        e.setStudentRefundMinor(e.getAmountMinor());
        e.setReleasedAt(Instant.now());
        escrowRepository.saveAndFlush(e);

        enqueueEvent(e.getId(), bookingId.toString(), EventTopics.PAYMENT_ESCROW_RELEASED,
                new EscrowReleasedPayload(e.getId(), bookingId, e.getStudentId(), e.getProfId(),
                        "STUDENT", null, e.getAmountMinor(), "MAD"));
    }

    /**
     * On {@code booking.dispute_resolved}: apply the admin-chosen split.
     *
     * @param resolution one of {@code RESOLVED_REFUND} / {@code RESOLVED_PAY_PROF} / {@code RESOLVED_SPLIT}.
     */
    @Transactional
    public void onDisputeResolved(UUID bookingId, String resolution,
                                  Long profShareMinor, Long studentRefundMinor, UUID traceId) {
        Escrow e = escrowRepository.findByBookingId(bookingId).orElse(null);
        if (e == null) {
            log.warn("dispute_resolved with no escrow — bookingId={}", bookingId);
            return;
        }
        if (e.getStatus() != EscrowStatus.LOCKED) {
            log.debug("Escrow not LOCKED ({}), skipping dispute resolution", e.getStatus());
            return;
        }
        switch (resolution) {
            case "RESOLVED_REFUND" -> {
                balanceLedger.apply(e.getStudentId(), e.getAmountMinor(),
                        AuditOperation.REFUND, AuditReferenceType.ESCROW, e.getId(),
                        e.getStudentId(), "Dispute resolved: full refund", traceId);
                e.setStatus(EscrowStatus.REFUNDED_TO_STUDENT);
                e.setStudentRefundMinor(e.getAmountMinor());
            }
            case "RESOLVED_PAY_PROF" -> {
                long share = profShareMinor != null ? profShareMinor : e.getProfShareMinor();
                balanceLedger.apply(e.getProfId(), share,
                        AuditOperation.ESCROW_RELEASE, AuditReferenceType.ESCROW, e.getId(),
                        e.getProfId(), "Dispute resolved: paid to PROF", traceId);
                e.setStatus(EscrowStatus.RELEASED_TO_PROF);
                e.setProfPayoutMinor(share);
            }
            case "RESOLVED_SPLIT" -> {
                if (profShareMinor == null || studentRefundMinor == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                            "SPLIT requires both profShareMinor and studentRefundMinor",
                            Map.of("bookingId", bookingId));
                }
                if (Math.addExact(profShareMinor, studentRefundMinor) != e.getAmountMinor()) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                            "SPLIT amounts do not sum to escrow amount",
                            Map.of("expected", e.getAmountMinor(),
                                    "got", profShareMinor + studentRefundMinor));
                }
                if (profShareMinor > 0) {
                    balanceLedger.apply(e.getProfId(), profShareMinor,
                            AuditOperation.ESCROW_RELEASE, AuditReferenceType.ESCROW, e.getId(),
                            e.getProfId(), "Dispute resolved: SPLIT (prof share)", traceId);
                }
                if (studentRefundMinor > 0) {
                    balanceLedger.apply(e.getStudentId(), studentRefundMinor,
                            AuditOperation.REFUND, AuditReferenceType.ESCROW, e.getId(),
                            e.getStudentId(), "Dispute resolved: SPLIT (student refund)", traceId);
                }
                e.setStatus(EscrowStatus.SPLIT);
                e.setProfPayoutMinor(profShareMinor);
                e.setStudentRefundMinor(studentRefundMinor);
            }
            default -> throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Unknown dispute resolution: " + resolution);
        }
        e.setReleasedAt(Instant.now());
        escrowRepository.saveAndFlush(e);

        String target = switch (e.getStatus()) {
            case RELEASED_TO_PROF -> "PROF";
            case REFUNDED_TO_STUDENT -> "STUDENT";
            case SPLIT -> "SPLIT";
            default -> "UNKNOWN";
        };
        enqueueEvent(e.getId(), bookingId.toString(), EventTopics.PAYMENT_ESCROW_RELEASED,
                new EscrowReleasedPayload(e.getId(), bookingId, e.getStudentId(), e.getProfId(),
                        target, e.getProfPayoutMinor(), e.getStudentRefundMinor(), "MAD"));
    }

    private long computeFee(long amountMinor) {
        // Integer math: bps / 10_000. Floor (round toward zero) so the prof never
        // receives more than `amount - fee`.
        long fee = amountMinor * (long) properties.getPlatformFeeBps() / 10_000L;
        if (fee < 0) fee = 0;
        if (fee > amountMinor) fee = amountMinor;
        return fee;
    }

    private void enqueueEvent(UUID aggregateId, String partitionKey,
                              String eventType, Object payload) {
        OutboxEvent row = envelopeBuilder.toOutbox(aggregateId, eventType, partitionKey, payload);
        outboxRepository.save(row);
    }

    private static BigDecimal toMajor(long minor) {
        return BigDecimal.valueOf(minor).movePointLeft(2);
    }
}

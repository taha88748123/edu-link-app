package ma.naja7i.payment.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.domain.AuditOperation;
import ma.naja7i.payment.domain.AuditReferenceType;
import ma.naja7i.payment.domain.Balance;
import ma.naja7i.payment.domain.OutboxEvent;
import ma.naja7i.payment.domain.Payout;
import ma.naja7i.payment.domain.PayoutStatus;
import ma.naja7i.payment.infrastructure.messaging.payload.PayoutDecidedPayload;
import ma.naja7i.payment.infrastructure.messaging.payload.PayoutRequestedPayload;
import ma.naja7i.payment.infrastructure.persistence.BalanceRepository;
import ma.naja7i.payment.infrastructure.persistence.OutboxRepository;
import ma.naja7i.payment.infrastructure.persistence.PayoutRepository;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PayoutService {

    public static final int MAX_PAGE_SIZE = 200;

    private final PayoutRepository payoutRepository;
    private final BalanceRepository balanceRepository;
    private final OutboxRepository outboxRepository;
    private final BalanceLedger balanceLedger;
    private final KafkaEventEnvelopeBuilder envelopeBuilder;

    // ============================================================ PROF

    /**
     * Prof requests a payout. Verifies the wallet has enough balance, but does
     * NOT debit yet — the debit happens when an admin approves.
     */
    @Transactional
    public Payout request(UUID profId, long amountMinor, String ibanRef) {
        if (amountMinor <= 0L) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Amount must be positive", Map.of("amountMinor", amountMinor));
        }
        long currentBalance = balanceRepository.findById(profId)
                .map(Balance::getBalanceMinor).orElse(0L);
        if (currentBalance < amountMinor) {
            throw new BusinessException(ErrorCode.PAYMENT_INSUFFICIENT_BALANCE,
                    "Insufficient balance for payout request",
                    Map.of("requested", amountMinor, "balance", currentBalance));
        }
        Payout p = payoutRepository.saveAndFlush(Payout.builder()
                .id(UUID.randomUUID())
                .profId(profId)
                .requestedAmountMinor(amountMinor)
                .currency("MAD")
                .status(PayoutStatus.REQUESTED)
                .ibanRef(ibanRef)
                .build());

        OutboxEvent row = envelopeBuilder.toOutbox(p.getId(),
                EventTopics.PAYMENT_PAYOUT_REQUESTED, profId.toString(),
                new PayoutRequestedPayload(p.getId(), profId,
                        toMajor(amountMinor), amountMinor, ibanRef));
        outboxRepository.save(row);
        return p;
    }

    @Transactional(readOnly = true)
    public Page<Payout> listForProf(UUID profId, PayoutStatus status, int page, int size) {
        return payoutRepository.findForProf(profId, status,
                clamp(page, size, Sort.by(Sort.Direction.DESC, "requestedAt")));
    }

    // ============================================================ ADMIN

    @Transactional(readOnly = true)
    public Page<Payout> listForAdmin(PayoutStatus status, int page, int size) {
        return payoutRepository.findForAdmin(status,
                clamp(page, size, Sort.by(Sort.Direction.DESC, "requestedAt")));
    }

    /**
     * Admin APPROVE / REJECT.
     *
     * <p>APPROVE: debits the prof's balance (via {@link BalanceLedger}) and
     * transitions to APPROVED. Status PAID is reached only later via
     * {@link #markPaid}.
     *
     * <p>REJECT: leaves the balance untouched.
     */
    @Transactional
    public Payout decide(UUID payoutId, UUID adminId, String decision,
                         String notes, UUID traceId) {
        Payout p = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Payout not found", Map.of("payoutId", payoutId)));
        if (p.getStatus() != PayoutStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "Payout not in REQUESTED state",
                    Map.of("payoutId", payoutId, "status", p.getStatus()));
        }
        switch (decision) {
            case "APPROVE" -> {
                // The debit is atomic with an audit_log row through BalanceLedger.
                balanceLedger.apply(p.getProfId(), -p.getRequestedAmountMinor(),
                        AuditOperation.PAYOUT, AuditReferenceType.PAYOUT, p.getId(),
                        adminId, "Payout approved by admin", traceId);
                p.setStatus(PayoutStatus.APPROVED);
                p.setApprovedAmountMinor(p.getRequestedAmountMinor());
            }
            case "REJECT" -> {
                p.setStatus(PayoutStatus.REJECTED);
                p.setApprovedAmountMinor(0L);
            }
            default -> throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Unknown decision: " + decision);
        }
        p.setAdminNotes(notes);
        p.setDecidedBy(adminId);
        p.setDecidedAt(Instant.now());
        payoutRepository.saveAndFlush(p);

        Long amount = p.getApprovedAmountMinor();
        OutboxEvent row = envelopeBuilder.toOutbox(p.getId(),
                EventTopics.PAYMENT_PAYOUT_DECIDED, p.getProfId().toString(),
                new PayoutDecidedPayload(p.getId(), p.getProfId(), decision,
                        amount == null ? null : toMajor(amount), amount, null));
        outboxRepository.save(row);
        return p;
    }

    /**
     * Admin confirms the bank transfer is done. Only valid on APPROVED payouts.
     * Does not touch the balance — that happened on APPROVE.
     */
    @Transactional
    public Payout markPaid(UUID payoutId, UUID adminId, String paymentRef) {
        Payout p = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Payout not found", Map.of("payoutId", payoutId)));
        if (p.getStatus() != PayoutStatus.APPROVED) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "Payout not in APPROVED state",
                    Map.of("payoutId", payoutId, "status", p.getStatus()));
        }
        p.setStatus(PayoutStatus.PAID);
        p.setPaymentRef(paymentRef);
        p.setPaidAt(Instant.now());
        payoutRepository.saveAndFlush(p);

        OutboxEvent row = envelopeBuilder.toOutbox(p.getId(),
                EventTopics.PAYMENT_PAYOUT_DECIDED, p.getProfId().toString(),
                new PayoutDecidedPayload(p.getId(), p.getProfId(), "PAID",
                        toMajor(p.getApprovedAmountMinor()), p.getApprovedAmountMinor(),
                        paymentRef));
        outboxRepository.save(row);
        return p;
    }

    @Transactional(readOnly = true)
    public Payout get(UUID id) {
        return payoutRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Payout not found", Map.of("payoutId", id)));
    }

    private static Pageable clamp(int page, int size, Sort sort) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize, sort);
    }

    private static BigDecimal toMajor(long minor) {
        return BigDecimal.valueOf(minor).movePointLeft(2);
    }
}

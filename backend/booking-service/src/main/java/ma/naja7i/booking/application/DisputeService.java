package ma.naja7i.booking.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.booking.config.BookingProperties;
import ma.naja7i.booking.domain.Booking;
import ma.naja7i.booking.domain.Dispute;
import ma.naja7i.booking.domain.DisputeStatus;
import ma.naja7i.booking.infrastructure.messaging.payload.BookingDisputedPayload;
import ma.naja7i.booking.infrastructure.messaging.payload.DisputeResolvedPayload;
import ma.naja7i.booking.infrastructure.persistence.BookingRepository;
import ma.naja7i.booking.infrastructure.persistence.BookingStateTransitionRepository;
import ma.naja7i.booking.infrastructure.persistence.DisputeRepository;
import ma.naja7i.booking.domain.BookingStateTransition;
import ma.naja7i.booking.infrastructure.web.dto.ResolveDisputeRequest;
import ma.naja7i.shared.dto.BookingStatus;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final BookingRepository bookingRepository;
    private final BookingStateTransitionRepository transitionRepository;
    private final BookingService bookingService; // for shared outbox helper
    private final BookingProperties properties;

    /**
     * Opens a dispute on a booking. Allowed when the booking is in EN_COURS or TERMINEE,
     * or CONFIRMEE if within the dispute window ({@code naja7i.booking.dispute-window-hours}).
     */
    @Transactional
    public Dispute openDispute(UUID bookingId, UUID actorId, Role actorRole, String cause,
                               String evidenceUrlsJson) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Booking not found", Map.of("bookingId", bookingId)));
        // Authorisation: student/prof of the booking, or ADMIN.
        boolean isOwner = (actorRole == Role.STUDENT && b.getStudentId().equals(actorId))
                || (actorRole == Role.PROF && b.getProfId().equals(actorId));
        if (actorRole != Role.ADMIN && !isOwner) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE,
                    "Not your booking", Map.of("bookingId", bookingId));
        }
        // Status guard.
        BookingStatus s = b.getStatus();
        boolean allowed = s == BookingStatus.EN_COURS || s == BookingStatus.TERMINEE;
        if (s == BookingStatus.CONFIRMEE) {
            Instant confirmed = b.getConfirmedAt() == null ? b.getUpdatedAt() : b.getConfirmedAt();
            Instant deadline = confirmed.plus(
                    Duration.ofHours(properties.getDisputeWindowHours()));
            if (Instant.now().isBefore(deadline)) {
                allowed = true;
            }
        }
        if (!allowed) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "Dispute window closed or invalid status",
                    Map.of("bookingId", bookingId, "status", s));
        }
        // One dispute per booking.
        if (disputeRepository.findByBookingId(bookingId).isPresent()) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "A dispute already exists for this booking",
                    Map.of("bookingId", bookingId));
        }
        Dispute d = disputeRepository.save(Dispute.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .openedBy(actorId)
                .cause(cause)
                .evidenceUrls(evidenceUrlsJson)
                .status(DisputeStatus.OPEN)
                .build());

        BookingStatus from = b.getStatus();
        b.setStatus(BookingStatus.LITIGE);
        bookingRepository.save(b);

        transitionRepository.save(BookingStateTransition.builder()
                .bookingId(bookingId)
                .fromStatus(from)
                .toStatus(BookingStatus.LITIGE)
                .triggeredBy(actorId)
                .reason("Dispute opened: " + cause)
                .build());

        bookingService.enqueueOutbox(bookingId, EventTopics.BOOKING_DISPUTED,
                new BookingDisputedPayload(d.getId(), bookingId, cause));
        return d;
    }

    /**
     * Admin resolution. Emits {@code naja7i.booking.dispute_resolved.v1} so that
     * payment-service can actually move the money.
     */
    @Transactional
    public Dispute resolve(UUID bookingId, UUID disputeId, UUID adminId,
                           ResolveDisputeRequest req) {
        Booking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Booking not found", Map.of("bookingId", bookingId)));
        Dispute d = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Dispute not found", Map.of("disputeId", disputeId)));
        if (!d.getBookingId().equals(bookingId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Dispute not found", Map.of("disputeId", disputeId));
        }
        if (d.getStatus() != DisputeStatus.OPEN && d.getStatus() != DisputeStatus.UNDER_REVIEW) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "Dispute already resolved",
                    Map.of("disputeId", disputeId, "status", d.getStatus()));
        }
        Long profShareMinor = toCentimes(req.profShareMad());
        Long studentRefundMinor = toCentimes(req.studentRefundMad());
        DisputeStatus newStatus = switch (req.decision()) {
            case ResolveDisputeRequest.DECISION_REFUND   -> DisputeStatus.RESOLVED_REFUND;
            case ResolveDisputeRequest.DECISION_PAY_PROF -> DisputeStatus.RESOLVED_PAY_PROF;
            case ResolveDisputeRequest.DECISION_SPLIT    -> DisputeStatus.RESOLVED_SPLIT;
            default -> throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Unknown decision: " + req.decision());
        };
        if (newStatus == DisputeStatus.RESOLVED_SPLIT
                && (profShareMinor == null || studentRefundMinor == null)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "SPLIT requires profShareMad + studentRefundMad");
        }
        d.setStatus(newStatus);
        d.setResolutionNotes(req.notes());
        d.setResolvedBy(adminId);
        d.setResolvedAt(Instant.now());
        disputeRepository.save(d);
        // booking state stays LITIGE (closed by the dispute resolution event downstream)

        bookingService.enqueueOutbox(bookingId, EventTopics.BOOKING_DISPUTE_RESOLVED,
                new DisputeResolvedPayload(d.getId(), bookingId, newStatus.name(),
                        profShareMinor, studentRefundMinor));
        return d;
    }

    private static Long toCentimes(BigDecimal majorAmount) {
        if (majorAmount == null) return null;
        return majorAmount.movePointRight(2).longValueExact();
    }
}

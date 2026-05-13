package ma.naja7i.booking.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import ma.naja7i.shared.dto.BookingStatus;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;

import java.time.Instant;
import java.util.UUID;

/**
 * Detailed booking view including audit timestamps and amounts.
 *
 * <p>ANTI-BYPASS: this DTO intentionally exposes ONLY opaque UUIDs for both the
 * student and the prof. Phone / email / display name are NEVER included — those
 * are user-profile-service's concern, and at the booking-service level we never
 * carry them around, regardless of {@link BookingStatus}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingDetailedView(
        UUID id,
        UUID studentId,
        UUID profId,
        UUID slotId,
        String subjectCode,
        SchoolLevel level,
        Modality modality,
        Short durationMinutes,
        long amountMinor,
        long platformFeeMinor,
        long totalAmountMinor,
        String currency,
        UUID escrowId,
        BookingStatus status,
        Instant scheduledStart,
        Instant scheduledEnd,
        Instant actualStart,
        Instant actualEnd,
        String cancelReason,
        UUID cancelledBy,
        Instant cancelledAt,
        Instant confirmedAt,
        String declineReason,
        Instant declinedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

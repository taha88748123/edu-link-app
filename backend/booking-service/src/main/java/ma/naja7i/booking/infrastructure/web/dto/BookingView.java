package ma.naja7i.booking.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import ma.naja7i.shared.dto.BookingStatus;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;

import java.time.Instant;
import java.util.UUID;

/**
 * Compact booking listing item. Anti-bypass note: only opaque UUIDs are exposed
 * (no phone/email regardless of viewer role).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingView(
        UUID id,
        UUID studentId,
        UUID profId,
        UUID slotId,
        String subjectCode,
        SchoolLevel level,
        Modality modality,
        Short durationMinutes,
        long totalAmountMinor,
        String currency,
        BookingStatus status,
        Instant scheduledStart,
        Instant scheduledEnd,
        Instant createdAt
) {
}

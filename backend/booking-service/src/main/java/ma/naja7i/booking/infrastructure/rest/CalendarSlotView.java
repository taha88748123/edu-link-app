package ma.naja7i.booking.infrastructure.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ma.naja7i.shared.dto.Modality;

import java.time.Instant;
import java.util.UUID;

/**
 * Subset of {@code calendar-service}'s {@code SlotView} that booking-service needs.
 * Extra fields are tolerated — we never deserialise the full source DTO so this
 * client remains decoupled from the calendar-service classpath.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CalendarSlotView(
        UUID id,
        UUID profId,
        Instant startAt,
        Instant endAt,
        Modality modality,
        String status,
        UUID bookingId,
        Instant reservedUntil
) {
}

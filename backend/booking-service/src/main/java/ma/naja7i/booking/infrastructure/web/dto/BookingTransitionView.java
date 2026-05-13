package ma.naja7i.booking.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import ma.naja7i.shared.dto.BookingStatus;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingTransitionView(
        Long id,
        UUID bookingId,
        BookingStatus fromStatus,
        BookingStatus toStatus,
        UUID triggeredBy,
        String reason,
        Instant createdAt
) {
}

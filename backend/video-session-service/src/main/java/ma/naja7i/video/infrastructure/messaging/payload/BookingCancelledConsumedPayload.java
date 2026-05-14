package ma.naja7i.video.infrastructure.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Subset of {@code naja7i.booking.cancelled.v1}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingCancelledConsumedPayload(UUID bookingId, UUID cancelledBy, String reason) {
}

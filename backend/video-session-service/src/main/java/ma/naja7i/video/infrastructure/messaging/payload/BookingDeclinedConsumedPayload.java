package ma.naja7i.video.infrastructure.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Subset of {@code naja7i.booking.declined.v1}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingDeclinedConsumedPayload(UUID bookingId, String reason) {
}

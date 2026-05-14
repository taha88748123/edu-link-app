package ma.naja7i.chat.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

/** Subset of {@code naja7i.booking.accepted.v1} fields used by chat-service. */
public record BookingAcceptedInboundPayload(UUID bookingId, Instant acceptedAt) {
}

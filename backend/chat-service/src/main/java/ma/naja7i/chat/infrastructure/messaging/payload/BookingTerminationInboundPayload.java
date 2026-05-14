package ma.naja7i.chat.infrastructure.messaging.payload;

import java.util.UUID;

/**
 * Aggregated payload for the various booking-lifecycle events that close a chat
 * (declined / cancelled / confirmed). All we need is the {@code bookingId}; any
 * extra fields are tolerated by Jackson.
 */
public record BookingTerminationInboundPayload(UUID bookingId, String reason) {
}

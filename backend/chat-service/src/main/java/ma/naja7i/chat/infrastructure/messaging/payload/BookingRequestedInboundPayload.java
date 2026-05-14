package ma.naja7i.chat.infrastructure.messaging.payload;

import java.util.UUID;

/** Subset of {@code naja7i.booking.requested.v1} fields used by chat-service. */
public record BookingRequestedInboundPayload(
        UUID bookingId,
        UUID studentId,
        UUID profId
) {
}

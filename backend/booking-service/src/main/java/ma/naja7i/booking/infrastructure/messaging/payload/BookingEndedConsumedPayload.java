package ma.naja7i.booking.infrastructure.messaging.payload;

import java.util.UUID;

/**
 * Subset of {@code naja7i.video.session.ended.v1} that we care about: the bookingId
 * and the session duration. Extra fields in the producer's payload are ignored.
 */
public record BookingEndedConsumedPayload(
        UUID sessionId,
        UUID bookingId,
        Long durationSec
) {
}

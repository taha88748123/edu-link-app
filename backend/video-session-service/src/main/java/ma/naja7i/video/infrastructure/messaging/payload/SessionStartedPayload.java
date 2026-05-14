package ma.naja7i.video.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

/** Payload of {@code naja7i.video.session.started.v1}. */
public record SessionStartedPayload(UUID sessionId, UUID bookingId, Instant startAt) {
}

package ma.naja7i.video.infrastructure.messaging.payload;

import java.util.UUID;

/**
 * Payload of {@code naja7i.video.session.ended.v1}. Consumed by booking-service
 * to transition EN_COURS -> TERMINEE.
 */
public record SessionEndedPayload(UUID sessionId, UUID bookingId, Long durationSec, String recordingUrl) {
}

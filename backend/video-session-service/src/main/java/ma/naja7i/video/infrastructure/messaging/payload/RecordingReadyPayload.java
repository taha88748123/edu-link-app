package ma.naja7i.video.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

/** Payload of {@code naja7i.video.recording.ready.v1}. */
public record RecordingReadyPayload(UUID sessionId, UUID bookingId, String recordingUrl, Instant expiresAt) {
}

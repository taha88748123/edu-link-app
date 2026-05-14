package ma.naja7i.video.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Single consent record persisted inside the {@code consent_given_by} JSONB
 * array of a {@link Session}. Immutable.
 */
public record Consent(UUID userId, Instant at, ConsentScope scope) {
}

package ma.naja7i.ai.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumer-side mirror of {@code naja7i.profile.prof.validated.v1}.
 * Keep in sync with {@code user-profile-service / ProfValidatedPayload}.
 */
public record ProfValidatedConsumedPayload(
        UUID userId,
        UUID validatedBy,
        Instant at
) {
}

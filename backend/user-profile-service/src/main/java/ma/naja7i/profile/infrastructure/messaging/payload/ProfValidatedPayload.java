package ma.naja7i.profile.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

public record ProfValidatedPayload(
        UUID userId,
        UUID validatedBy,
        Instant at
) { }

package ma.naja7i.profile.infrastructure.messaging.payload;

import java.util.UUID;

public record ProfRejectedPayload(
        UUID userId,
        String reason
) { }

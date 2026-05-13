package ma.naja7i.auth.events.payload;

import java.util.UUID;

public record UserSuspendedPayload(
        UUID userId,
        String reason
) {}

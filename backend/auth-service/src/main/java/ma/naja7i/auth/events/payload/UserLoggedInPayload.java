package ma.naja7i.auth.events.payload;

import java.time.Instant;
import java.util.UUID;

public record UserLoggedInPayload(
        UUID userId,
        String ip,
        String userAgent,
        Instant at
) {}

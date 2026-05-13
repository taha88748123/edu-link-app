package ma.naja7i.auth.events.payload;

import java.time.Instant;
import java.util.UUID;

public record PasswordResetRequestedPayload(
        UUID userId,
        String resetToken,
        Instant expiresAt
) {}

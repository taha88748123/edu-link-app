package ma.naja7i.notification.infrastructure.messaging.incoming;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumer-side mirrors of the {@code auth-service} event payloads, kept in this
 * package because the notification-service does NOT depend on the producer's types.
 * Field shapes mirror {@code docs/architecture/events-catalog.md} — keep in sync.
 */
public final class AuthEvents {

    private AuthEvents() {}

    /** {@code naja7i.auth.otp.requested.v1} */
    public record OtpRequested(UUID userId, String code, String channel, Instant expiresAt) {}

    /** {@code naja7i.auth.user.registered.v1} */
    public record UserRegistered(UUID userId, String role, String email, String phone) {}

    /** {@code naja7i.auth.password.reset_requested.v1} */
    public record PasswordResetRequested(UUID userId, String resetToken, Instant expiresAt) {}
}

package ma.naja7i.booking.infrastructure.messaging.payload;

import java.util.UUID;

/** Subset of {@code naja7i.auth.user.suspended.v1} fields used by booking-service. */
public record UserSuspendedPayload(UUID userId, String role, String reason) {}

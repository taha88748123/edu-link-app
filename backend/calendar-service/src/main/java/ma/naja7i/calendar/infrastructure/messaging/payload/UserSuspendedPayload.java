package ma.naja7i.calendar.infrastructure.messaging.payload;

import java.util.UUID;

/** Subset of {@code naja7i.auth.user.suspended.v1} fields. */
public record UserSuspendedPayload(UUID userId, String reason) {}

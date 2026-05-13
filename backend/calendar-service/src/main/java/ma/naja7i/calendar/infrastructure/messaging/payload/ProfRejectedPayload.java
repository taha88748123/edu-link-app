package ma.naja7i.calendar.infrastructure.messaging.payload;

import java.util.UUID;

/** Subset of {@code naja7i.profile.prof.rejected.v1} fields. */
public record ProfRejectedPayload(UUID userId, String reason) {}

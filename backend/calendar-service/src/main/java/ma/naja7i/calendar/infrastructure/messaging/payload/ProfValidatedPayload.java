package ma.naja7i.calendar.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

/** Subset of {@code naja7i.profile.prof.validated.v1} fields we care about. */
public record ProfValidatedPayload(UUID userId, UUID validatedBy, Instant at) {}

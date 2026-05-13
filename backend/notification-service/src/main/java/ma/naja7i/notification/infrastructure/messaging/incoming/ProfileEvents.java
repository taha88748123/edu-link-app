package ma.naja7i.notification.infrastructure.messaging.incoming;

import java.util.List;
import java.util.UUID;

/**
 * Consumer-side mirrors of the {@code user-profile-service} event payloads.
 * Keep in sync with {@code docs/architecture/events-catalog.md}.
 */
public final class ProfileEvents {

    private ProfileEvents() {}

    /** {@code naja7i.profile.prof.submitted.v1} */
    public record ProfSubmitted(UUID userId, String displayName, List<String> subjects, int diplomasCount) {}

    /** {@code naja7i.profile.prof.evaluated.v1} */
    public record ProfEvaluated(UUID userId, double qcmScore, String decision) {}

    /** {@code naja7i.profile.prof.validated.v1} */
    public record ProfValidated(UUID userId, UUID validatedBy, java.time.Instant at) {}

    /** {@code naja7i.profile.prof.rejected.v1} */
    public record ProfRejected(UUID userId, String reason) {}
}

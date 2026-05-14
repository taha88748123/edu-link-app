package ma.naja7i.ai.infrastructure.messaging.payload;

import java.util.List;
import java.util.UUID;

/**
 * Consumer-side mirror of {@code naja7i.profile.prof.submitted.v1}.
 */
public record ProfSubmittedConsumedPayload(
        UUID userId,
        String displayName,
        List<String> subjects,
        int diplomasCount
) {
}

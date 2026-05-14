package ma.naja7i.ai.infrastructure.messaging.payload;

import java.util.UUID;

/**
 * Consumer-side mirror of {@code naja7i.profile.review.created.v1}.
 * In v1 we no-op on receipt; recomputing the prof rating from this signal
 * is a v2 concern.
 */
public record ReviewCreatedConsumedPayload(
        UUID reviewId,
        UUID profId,
        UUID studentId,
        int stars,
        String comment
) {
}

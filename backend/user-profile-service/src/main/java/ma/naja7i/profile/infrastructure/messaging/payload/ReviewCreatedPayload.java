package ma.naja7i.profile.infrastructure.messaging.payload;

import java.util.UUID;

public record ReviewCreatedPayload(
        UUID reviewId,
        UUID profId,
        UUID studentId,
        short stars,
        String comment
) { }

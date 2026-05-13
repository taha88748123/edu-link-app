package ma.naja7i.profile.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewView(
        UUID id,
        UUID bookingId,
        UUID profId,
        UUID studentId,
        int stars,
        String comment,
        boolean moderated,
        Instant createdAt
) { }

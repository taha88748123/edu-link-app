package ma.naja7i.profile.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

public record DiplomaView(
        UUID id,
        UUID userId,
        String title,
        String institution,
        Integer year,
        String fileUrl,
        boolean verified,
        Instant createdAt
) { }

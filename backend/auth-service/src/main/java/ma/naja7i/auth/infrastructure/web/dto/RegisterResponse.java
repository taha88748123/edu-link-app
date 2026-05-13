package ma.naja7i.auth.infrastructure.web.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        String status,
        String message
) {}

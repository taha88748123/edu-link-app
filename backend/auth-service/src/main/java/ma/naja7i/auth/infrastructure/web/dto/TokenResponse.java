package ma.naja7i.auth.infrastructure.web.dto;

import ma.naja7i.shared.dto.Role;

import java.util.UUID;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresInSec,
        UUID userId,
        Role role
) {}

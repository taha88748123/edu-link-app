package ma.naja7i.auth.infrastructure.web.dto;

import ma.naja7i.auth.domain.UserStatus;
import ma.naja7i.shared.dto.Role;

import java.time.Instant;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String phone,
        Role role,
        UserStatus status,
        String locale,
        boolean emailVerified,
        boolean phoneVerified,
        Instant lastLoginAt
) {}

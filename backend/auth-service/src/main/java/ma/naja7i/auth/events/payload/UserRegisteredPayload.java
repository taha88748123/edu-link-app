package ma.naja7i.auth.events.payload;

import ma.naja7i.shared.dto.Role;

import java.util.UUID;

public record UserRegisteredPayload(
        UUID userId,
        Role role,
        String email,
        String phone
) {}

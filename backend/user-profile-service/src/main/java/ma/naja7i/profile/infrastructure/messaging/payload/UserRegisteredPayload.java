package ma.naja7i.profile.infrastructure.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ma.naja7i.shared.dto.Role;

import java.util.UUID;

/**
 * Payload of {@code naja7i.auth.user.registered.v1}.
 * Fields are kept loose so we tolerate forward-compatible additions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserRegisteredPayload(
        UUID userId,
        Role role,
        String email,
        String phone,
        String locale
) { }

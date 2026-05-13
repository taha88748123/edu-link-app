package ma.naja7i.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank
        String phoneOrEmail,

        @NotBlank
        String password
) {}

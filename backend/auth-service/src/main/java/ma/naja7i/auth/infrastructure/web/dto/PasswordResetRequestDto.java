package ma.naja7i.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequestDto(
        @NotBlank
        String phoneOrEmail
) {}

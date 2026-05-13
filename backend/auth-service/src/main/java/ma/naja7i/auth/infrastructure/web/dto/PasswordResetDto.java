package ma.naja7i.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetDto(
        @NotBlank
        String resetToken,

        @NotBlank
        @Size(min = 8, max = 128)
        String newPassword
) {}

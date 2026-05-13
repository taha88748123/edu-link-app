package ma.naja7i.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record VerifyOtpRequest(
        @NotNull
        UUID userId,

        @NotBlank
        @Pattern(regexp = "^[0-9]{4,8}$", message = "OTP must be a numeric code")
        String code
) {}

package ma.naja7i.auth.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ma.naja7i.shared.dto.Role;

public record RegisterRequest(
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Invalid phone number")
        String phone,

        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 128, message = "Password must be 8-128 characters")
        String password,

        @NotNull
        Role role,

        @Size(min = 2, max = 5)
        String locale
) {
    public String localeOrDefault() {
        return locale == null || locale.isBlank() ? "fr" : locale;
    }
}

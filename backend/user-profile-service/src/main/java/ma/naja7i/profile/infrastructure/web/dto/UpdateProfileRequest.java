package ma.naja7i.profile.infrastructure.web.dto;

import jakarta.validation.constraints.Size;
import ma.naja7i.shared.i18n.SupportedLocale;

import java.util.Map;

public record UpdateProfileRequest(
        @Size(max = 128) String displayName,
        @Size(max = 4000) String bio,
        String photoUrl,
        SupportedLocale preferredLanguage,
        Map<String, Object> notificationPrefs
) { }

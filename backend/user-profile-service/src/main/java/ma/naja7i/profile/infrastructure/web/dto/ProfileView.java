package ma.naja7i.profile.infrastructure.web.dto;

import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.i18n.SupportedLocale;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ProfileView(
        UUID userId,
        Role role,
        String displayName,
        String photoUrl,
        String bio,
        SupportedLocale preferredLanguage,
        Map<String, Object> notificationPrefs,
        Instant createdAt,
        Instant updatedAt,
        ProfPrivateView prof,
        StudentView student
) { }

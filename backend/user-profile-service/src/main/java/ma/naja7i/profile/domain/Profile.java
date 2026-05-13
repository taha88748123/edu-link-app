package ma.naja7i.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.i18n.SupportedLocale;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(columnDefinition = "text")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", nullable = false)
    private SupportedLocale preferredLanguage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notification_prefs", columnDefinition = "jsonb")
    private Map<String, Object> notificationPrefs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.preferredLanguage == null) this.preferredLanguage = SupportedLocale.FR;
        if (this.notificationPrefs == null) {
            this.notificationPrefs = new HashMap<>(Map.of(
                    "push", true, "sms", true, "wa", true, "email", true));
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

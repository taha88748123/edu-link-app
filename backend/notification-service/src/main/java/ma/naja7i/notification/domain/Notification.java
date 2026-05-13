package ma.naja7i.notification.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted record of one outbound notification (audit + observability).
 * One row per (user, channel, template) attempt.
 */
@Entity
@Table(name = "notification_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 16, nullable = false)
    private Channel channel;

    @Column(name = "template_key", length = 64, nullable = false)
    private String templateKey;

    @Column(name = "locale", length = 8, nullable = false)
    private String locale;

    /** Rendered subject (nullable for SMS/Push). */
    @Column(name = "subject", length = 255)
    private String subject;

    /** Rendered body. */
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /** Raw template parameters as JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private NotificationStatus status;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "error", length = 1000)
    private String error;

    @Column(name = "attempts", nullable = false)
    private short attempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (status == null) status = NotificationStatus.QUEUED;
    }

    @PreUpdate
    public void onUpdate() {
        /* nothing for now — sentAt/deliveredAt set by services */
    }

    @JsonIgnore
    public boolean isTerminal() {
        return status == NotificationStatus.SENT
                || status == NotificationStatus.DELIVERED
                || status == NotificationStatus.READ
                || status == NotificationStatus.FAILED;
    }
}

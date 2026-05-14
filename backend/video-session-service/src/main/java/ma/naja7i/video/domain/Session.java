package ma.naja7i.video.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A video session control-plane aggregate. One row per booking. The actual
 * media flows between clients and LiveKit; this row records lifecycle, consent
 * and (later) recording metadata.
 *
 * <p>Lifecycle:
 * <pre>
 *   SCHEDULED -> LIVE      (participant :start)
 *   LIVE      -> ENDED     (participant :end)
 *   SCHEDULED -> EXPIRED   (admin or booking-cancelled consumer)
 *   *         -> FAILED    (reserved for future error handling)
 * </pre>
 *
 * <p>Recording-related fields are populated post-session; they MAY be exposed
 * only if both participants previously consented (RECORDING).
 */
@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true, updatable = false)
    private UUID bookingId;

    @Column(name = "prof_id", nullable = false, updatable = false)
    private UUID profId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Column(name = "livekit_room", nullable = false, length = 100, unique = true, updatable = false)
    private String livekitRoom;

    @Column(name = "scheduled_start", nullable = false)
    private Instant scheduledStart;

    @Column(name = "scheduled_end", nullable = false)
    private Instant scheduledEnd;

    @Column(name = "actual_start")
    private Instant actualStart;

    @Column(name = "actual_end")
    private Instant actualEnd;

    @Column(name = "duration_effective_seconds")
    private Integer durationEffectiveSeconds;

    @Column(name = "recording_enabled", nullable = false)
    private Boolean recordingEnabled;

    @ToString.Exclude
    @Column(name = "recording_url", length = 500)
    private String recordingUrl;

    @Column(name = "recording_expires_at")
    private Instant recordingExpiresAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "consent_given_by", columnDefinition = "jsonb")
    private List<Consent> consentGivenBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (version == null) version = 0;
        if (recordingEnabled == null) recordingEnabled = Boolean.FALSE;
        if (consentGivenBy == null) consentGivenBy = new ArrayList<>();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

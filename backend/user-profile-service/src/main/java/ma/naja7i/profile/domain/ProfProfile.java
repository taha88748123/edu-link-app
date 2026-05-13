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
import ma.naja7i.shared.dto.ProfStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prof_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfProfile {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfStatus status;

    @Column(name = "intro_video_url")
    private String introVideoUrl;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Column(name = "ai_qcm_score")
    private BigDecimal aiQcmScore;

    @Column(name = "admin_notes", columnDefinition = "text")
    private String adminNotes;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "validated_by")
    private UUID validatedBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = ProfStatus.EN_ATTENTE;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

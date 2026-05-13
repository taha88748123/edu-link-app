package ma.naja7i.pricing.domain;

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
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;

import java.time.Instant;
import java.util.UUID;

/**
 * A single-session rate row for a prof.
 * Stored amount is in centimes (BIGINT) — see {@link ma.naja7i.shared.dto.Money}.
 */
@Entity
@Table(name = "prof_pricing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfPricing {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "prof_id", nullable = false)
    private UUID profId;

    @Column(name = "subject_code", nullable = false, length = 32)
    private String subjectCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private SchoolLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "modality", nullable = false, length = 20)
    private Modality modality;

    @Column(name = "duration_minutes", nullable = false)
    private Short durationMinutes;

    /** {@code null} = applies to all days; otherwise 0=Mon ... 6=Sun. */
    @Column(name = "day_of_week")
    private Short dayOfWeek;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "active", nullable = false)
    private boolean active;

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
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (currency == null) currency = "MAD";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

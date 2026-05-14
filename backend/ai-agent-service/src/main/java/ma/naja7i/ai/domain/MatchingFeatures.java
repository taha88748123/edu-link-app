package ma.naja7i.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Denormalised feature cache used by the MATCHING agent. One row per user
 * (PROF or STUDENT). {@code features} is a JSONB blob whose shape depends
 * on the role:
 *
 * <ul>
 *   <li>PROF: {@code subjects:[], levels:[], modalities:[], rating:0.0,
 *       avg_price_minor:0, city:""}</li>
 *   <li>STUDENT: {@code target_exam:"", preferred_subjects:[],
 *       budget_max_minor:0, city:""}</li>
 * </ul>
 *
 * <p>Optimistic locking with {@link Version} guards concurrent recomputation
 * from multiple consumer replicas.</p>
 */
@Entity
@Table(name = "matching_features")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingFeatures {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", nullable = false, columnDefinition = "jsonb")
    private String features;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    void onCreate() {
        if (computedAt == null) computedAt = Instant.now();
        if (version == null) version = 0;
    }

    @PreUpdate
    void onUpdate() {
        if (computedAt == null) computedAt = Instant.now();
    }
}

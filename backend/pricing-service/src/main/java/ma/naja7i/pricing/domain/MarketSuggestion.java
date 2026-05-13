package ma.naja7i.pricing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
 * Read-only cache row populated by an external AI/batch job. Pricing-service only reads it.
 */
@Entity
@Table(name = "market_suggestions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketSuggestion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "prof_id")
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

    @Column(name = "suggested_amount_minor", nullable = false)
    private Long suggestedAmountMinor;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @Column(name = "source", nullable = false, length = 20)
    private String source;
}

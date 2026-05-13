package ma.naja7i.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "review_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewStat {

    @Id
    @Column(name = "prof_id", nullable = false, updatable = false)
    private UUID profId;

    @Column(name = "avg_stars", nullable = false, precision = 3, scale = 2)
    private BigDecimal avgStars;

    @Column(name = "count", nullable = false)
    private long count;
}

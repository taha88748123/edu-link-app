package ma.naja7i.pricing.domain;

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
import java.time.Instant;
import java.util.UUID;

/**
 * Platform commission configuration. v1 has a single active row named {@code STANDARD}.
 */
@Entity
@Table(name = "platform_fees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformFee {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /** Percentage applied to the base amount, e.g. {@code 10.00} = 10 %. */
    @Column(name = "percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal percent;

    /** Optional fixed surcharge in centimes, applied after the percentage. */
    @Column(name = "fixed_minor", nullable = false)
    private Long fixedMinor;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

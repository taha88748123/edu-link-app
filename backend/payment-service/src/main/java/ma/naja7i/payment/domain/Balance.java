package ma.naja7i.payment.domain;

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

import java.time.Instant;
import java.util.UUID;

/**
 * Wallet balance for a single user. Stored in MAD centimes (BIGINT). Negative
 * balance is impossible by DB CHECK constraint. Optimistic locking via {@link Version}.
 */
@Entity
@Table(name = "balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Balance {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "balance_minor", nullable = false)
    private Long balanceMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

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
        if (currency == null) currency = "MAD";
        if (balanceMinor == null) balanceMinor = 0L;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

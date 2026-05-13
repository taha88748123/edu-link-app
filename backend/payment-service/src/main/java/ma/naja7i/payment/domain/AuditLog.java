package ma.naja7i.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only ledger row recording every change to a {@link Balance}. The DB
 * has triggers that reject UPDATE/DELETE — we never call setters from Java
 * either, hence no {@code @PreUpdate}, no {@code updated_at}.
 *
 * <p>The {@code amountMinor} is signed: positive = credit, negative = debit.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 30, updatable = false)
    private AuditOperation operation;

    /** Signed: positive credit / negative debit. */
    @Column(name = "amount_minor", nullable = false, updatable = false)
    private Long amountMinor;

    @Column(name = "balance_before", nullable = false, updatable = false)
    private Long balanceBefore;

    @Column(name = "balance_after", nullable = false, updatable = false)
    private Long balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 20, updatable = false)
    private AuditReferenceType referenceType;

    @Column(name = "reference_id", nullable = false, updatable = false)
    private UUID referenceId;

    @Column(name = "reason", updatable = false)
    private String reason;

    @Column(name = "trace_id", updatable = false)
    private UUID traceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}

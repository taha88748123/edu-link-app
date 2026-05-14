package ma.naja7i.ai.domain;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only observability row. Inserted by {@code AiCallLogger} in its own
 * transaction so a logging failure never breaks the user-facing flow.
 *
 * <p><b>Privacy</b>: raw prompts and responses are NEVER stored; only their
 * SHA-256 hashes ({@code inputHash}, {@code outputHash}) — see the project
 * hard constraints.</p>
 */
@Entity
@Table(name = "ai_calls_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent", nullable = false, length = 20, updatable = false)
    private AiAgent agent;

    @Column(name = "model", nullable = false, length = 50, updatable = false)
    private String model;

    @Column(name = "input_hash", nullable = false, length = 64, updatable = false)
    private String inputHash;

    @Column(name = "output_hash", length = 64, updatable = false)
    private String outputHash;

    @Column(name = "tokens_in", nullable = false)
    private Integer tokensIn;

    @Column(name = "tokens_out", nullable = false)
    private Integer tokensOut;

    @Column(name = "latency_ms", nullable = false)
    private Integer latencyMs;

    @Column(name = "cost_usd", nullable = false, precision = 10, scale = 6)
    private BigDecimal costUsd;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "trace_id")
    private UUID traceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (tokensIn == null) tokensIn = 0;
        if (tokensOut == null) tokensOut = 0;
        if (latencyMs == null) latencyMs = 0;
        if (costUsd == null) costUsd = BigDecimal.ZERO;
        if (success == null) success = Boolean.TRUE;
    }
}

package ma.naja7i.ai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A QCM run instance for a prof. Scoped for v2 — the table is created so
 * v2 code can be added without a migration, but no business logic in v1
 * reads or mutates these rows.
 */
@Entity
@Table(name = "qcm_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QcmSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "prof_id", nullable = false)
    private UUID profId;

    @Column(name = "subject_code", nullable = false, length = 20)
    private String subjectCode;

    @Column(name = "level", nullable = false, length = 20)
    private String level;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generated_questions", nullable = false, columnDefinition = "jsonb")
    private String generatedQuestions;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;
}

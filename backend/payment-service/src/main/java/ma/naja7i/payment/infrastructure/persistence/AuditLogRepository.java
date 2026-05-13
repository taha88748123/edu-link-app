package ma.naja7i.payment.infrastructure.persistence;

import ma.naja7i.payment.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
           SELECT a FROM AuditLog a
           WHERE a.userId = :userId
             AND (:from IS NULL OR a.createdAt >= :from)
             AND (:to   IS NULL OR a.createdAt <= :to)
           """)
    Page<AuditLog> findByUserIdAndCreatedAtBetween(@Param("userId") UUID userId,
                                                   @Param("from") Instant from,
                                                   @Param("to") Instant to,
                                                   Pageable pageable);

    /**
     * Streaming variant for CSV export: ordered, eagerly loaded. Callers are
     * expected to cap the date range to avoid OOM (the controller enforces a
     * reasonable max).
     */
    @Query("""
           SELECT a FROM AuditLog a
           WHERE (:userId IS NULL OR a.userId = :userId)
             AND (:from IS NULL OR a.createdAt >= :from)
             AND (:to   IS NULL OR a.createdAt <= :to)
           ORDER BY a.createdAt ASC, a.id ASC
           """)
    List<AuditLog> findForExport(@Param("userId") UUID userId,
                                 @Param("from") Instant from,
                                 @Param("to") Instant to);
}

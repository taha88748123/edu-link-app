package ma.naja7i.ai.infrastructure.persistence;

import ma.naja7i.ai.domain.AiAgent;
import ma.naja7i.ai.domain.AiCallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long> {

    Page<AiCallLog> findByAgentAndCreatedAtBetween(AiAgent agent,
                                                   Instant from,
                                                   Instant to,
                                                   Pageable pageable);

    Page<AiCallLog> findByCreatedAtBetween(Instant from, Instant to, Pageable pageable);

    /**
     * Aggregated KPIs per agent. Returned as a list of Object[]:
     * {@code [agent, count, total_cost_usd, avg_latency, success_rate]}.
     */
    @Query("""
           SELECT l.agent,
                  COUNT(l),
                  COALESCE(SUM(l.costUsd), 0),
                  COALESCE(AVG(l.latencyMs), 0),
                  COALESCE(AVG(CASE WHEN l.success = TRUE THEN 1.0 ELSE 0.0 END), 0)
           FROM AiCallLog l
           WHERE l.createdAt BETWEEN :from AND :to
           GROUP BY l.agent
           ORDER BY l.agent
           """)
    List<Object[]> summarise(@Param("from") Instant from, @Param("to") Instant to);
}

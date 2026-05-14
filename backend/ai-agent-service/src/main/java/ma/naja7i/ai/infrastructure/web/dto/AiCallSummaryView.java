package ma.naja7i.ai.infrastructure.web.dto;

import java.math.BigDecimal;

/**
 * Aggregated KPI bucket per agent, used by the admin cost dashboard.
 * Matches the projection emitted by {@code AiCallLogRepository.summarise(...)}.
 */
public record AiCallSummaryView(
        String agent,
        long count,
        BigDecimal totalCostUsd,
        double avgLatencyMs,
        double successRate
) {
}

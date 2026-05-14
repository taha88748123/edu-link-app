package ma.naja7i.ai.infrastructure.web.dto;

import ma.naja7i.ai.domain.AiCallLog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Admin-facing read model of an {@code ai_calls_log} row. Hashes are
 * exposed (they reveal nothing about the prompt) but raw prompts /
 * responses are never persisted in the first place.
 */
public record AiCallEntryView(
        Long id,
        UUID userId,
        String agent,
        String model,
        String inputHash,
        String outputHash,
        int tokensIn,
        int tokensOut,
        int latencyMs,
        BigDecimal costUsd,
        boolean success,
        String errorCode,
        UUID traceId,
        Instant createdAt
) {

    public static AiCallEntryView from(AiCallLog row) {
        return new AiCallEntryView(
                row.getId(),
                row.getUserId(),
                row.getAgent() == null ? null : row.getAgent().name(),
                row.getModel(),
                row.getInputHash(),
                row.getOutputHash(),
                row.getTokensIn() == null ? 0 : row.getTokensIn(),
                row.getTokensOut() == null ? 0 : row.getTokensOut(),
                row.getLatencyMs() == null ? 0 : row.getLatencyMs(),
                row.getCostUsd() == null ? BigDecimal.ZERO : row.getCostUsd(),
                row.getSuccess() != null && row.getSuccess(),
                row.getErrorCode(),
                row.getTraceId(),
                row.getCreatedAt());
    }
}

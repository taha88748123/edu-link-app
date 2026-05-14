package ma.naja7i.ai.infrastructure.messaging.payload;

import java.math.BigDecimal;

/**
 * Payload for {@code naja7i.ai.call.completed.v1}. Consumed by the admin
 * dashboard for cost monitoring. Partition key on the Kafka side is the
 * agent name.
 */
public record AiCallCompletedPayload(
        String agent,
        String model,
        int tokensIn,
        int tokensOut,
        int latencyMs,
        BigDecimal costUsd,
        boolean success,
        String errorCode
) {
}

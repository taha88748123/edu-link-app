package ma.naja7i.payment.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.util.UUID;

/** Payload of {@code naja7i.payment.balance.adjusted.v1}. */
public record BalanceAdjustedPayload(
        UUID userId,
        UUID actorId,
        String operation,
        BigDecimal delta,
        long deltaMinor,
        String reason
) {
}

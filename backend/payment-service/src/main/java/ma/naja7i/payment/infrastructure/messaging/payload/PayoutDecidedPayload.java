package ma.naja7i.payment.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.util.UUID;

/** Payload of {@code naja7i.payment.payout.decided.v1}. */
public record PayoutDecidedPayload(
        UUID payoutId,
        UUID profId,
        String decision,
        BigDecimal amount,
        Long amountMinor,
        String paymentRef
) {
}

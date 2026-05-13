package ma.naja7i.payment.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.util.UUID;

/** Payload of {@code naja7i.payment.payout.requested.v1}. */
public record PayoutRequestedPayload(
        UUID payoutId,
        UUID profId,
        BigDecimal amount,
        long amountMinor,
        String ibanRef
) {
}

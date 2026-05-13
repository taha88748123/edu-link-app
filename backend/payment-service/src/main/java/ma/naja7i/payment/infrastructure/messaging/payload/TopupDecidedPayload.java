package ma.naja7i.payment.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.util.UUID;

/** Payload of {@code naja7i.payment.topup.decided.v1}. */
public record TopupDecidedPayload(
        UUID topupId,
        UUID studentId,
        String decision,
        BigDecimal approvedAmount,
        long approvedAmountMinor,
        UUID decidedBy
) {
}

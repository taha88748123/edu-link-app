package ma.naja7i.payment.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.util.UUID;

/** Payload of {@code naja7i.payment.topup.submitted.v1}. */
public record TopupSubmittedPayload(
        UUID topupId,
        UUID studentId,
        BigDecimal amount,
        long amountMinor,
        String method,
        String proofUrl
) {
}

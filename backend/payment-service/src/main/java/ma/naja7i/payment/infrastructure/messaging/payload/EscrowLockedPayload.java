package ma.naja7i.payment.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.util.UUID;

/** Payload of {@code naja7i.payment.escrow.locked.v1}. */
public record EscrowLockedPayload(
        UUID escrowId,
        UUID bookingId,
        UUID studentId,
        UUID profId,
        BigDecimal amount,
        long amountMinor,
        long platformFeeMinor,
        long profShareMinor,
        String currency
) {
}

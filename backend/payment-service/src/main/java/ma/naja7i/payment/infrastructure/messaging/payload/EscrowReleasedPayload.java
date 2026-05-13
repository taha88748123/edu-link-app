package ma.naja7i.payment.infrastructure.messaging.payload;

import java.util.UUID;

/**
 * Payload of {@code naja7i.payment.escrow.released.v1}.
 *
 * <p>{@code target} is one of {@code PROF}, {@code STUDENT}, {@code SPLIT}.
 * For SPLIT, both {@code profPayoutMinor} and {@code studentRefundMinor} are
 * populated; for PROF only {@code profPayoutMinor}, for STUDENT only
 * {@code studentRefundMinor}.
 */
public record EscrowReleasedPayload(
        UUID escrowId,
        UUID bookingId,
        UUID studentId,
        UUID profId,
        String target,
        Long profPayoutMinor,
        Long studentRefundMinor,
        String currency
) {
}

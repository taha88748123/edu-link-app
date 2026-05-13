package ma.naja7i.payment.infrastructure.messaging.payload;

import java.util.UUID;

/**
 * Inbound payload of {@code naja7i.booking.dispute_resolved.v1}.
 *
 * <p>{@code resolution} matches the booking-service's DisputeStatus enum names:
 * {@code RESOLVED_REFUND}, {@code RESOLVED_PAY_PROF}, {@code RESOLVED_SPLIT}.
 * For SPLIT the two minor amounts MUST sum to the locked escrow amount.
 */
public record DisputeResolvedConsumedPayload(
        UUID disputeId,
        UUID bookingId,
        String resolution,
        Long profShareMinor,
        Long studentRefundMinor
) {
}

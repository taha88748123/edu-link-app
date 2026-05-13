package ma.naja7i.booking.infrastructure.messaging.payload;

import java.util.UUID;

/**
 * Payload of {@code naja7i.booking.dispute_resolved.v1}.
 *
 * <p>The {@code resolution} field is one of {@code RESOLVED_REFUND},
 * {@code RESOLVED_PAY_PROF}, {@code RESOLVED_SPLIT}. Payment-service consumes
 * this event and moves the money accordingly.
 */
public record DisputeResolvedPayload(
        UUID disputeId,
        UUID bookingId,
        String resolution,
        Long profShareMinor,
        Long studentRefundMinor
) {
}

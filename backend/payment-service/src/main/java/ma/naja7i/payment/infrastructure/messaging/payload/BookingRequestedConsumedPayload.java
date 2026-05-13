package ma.naja7i.payment.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Inbound deserialisation of {@code naja7i.booking.requested.v1}. Mirrors the
 * producer side (booking-service.BookingRequestedPayload) but lives in the
 * consumer's package so we don't depend on a sibling module's internals.
 *
 * <p>Both {@code amount} (major) and {@code amountMinor} are present; the
 * consumer ALWAYS uses {@code amountMinor} (long centimes) for math.
 */
public record BookingRequestedConsumedPayload(
        UUID bookingId,
        UUID studentId,
        UUID profId,
        UUID slotId,
        BigDecimal amountMad,
        long amountMinor,
        String currency,
        Instant scheduledStart
) {
}

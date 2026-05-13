package ma.naja7i.booking.infrastructure.messaging.payload;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payload of {@code naja7i.booking.requested.v1}.
 *
 * <p>{@code amountMad} is the human-readable major amount; {@code amountMinor} is the
 * machine-precise centimes value. Consumers should rely on {@code amountMinor}.
 */
public record BookingRequestedPayload(
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

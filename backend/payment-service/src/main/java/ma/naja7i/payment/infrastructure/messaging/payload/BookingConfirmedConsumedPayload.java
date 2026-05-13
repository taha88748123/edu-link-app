package ma.naja7i.payment.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

public record BookingConfirmedConsumedPayload(UUID bookingId, Instant confirmedAt) {
}

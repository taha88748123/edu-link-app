package ma.naja7i.payment.infrastructure.messaging.payload;

import java.util.UUID;

public record BookingDeclinedConsumedPayload(UUID bookingId, String reason) {
}

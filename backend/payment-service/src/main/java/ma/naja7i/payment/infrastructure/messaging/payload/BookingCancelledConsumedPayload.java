package ma.naja7i.payment.infrastructure.messaging.payload;

import java.util.UUID;

public record BookingCancelledConsumedPayload(UUID bookingId, UUID cancelledBy, String reason) {
}

package ma.naja7i.booking.infrastructure.messaging.payload;

import java.util.UUID;

public record BookingDeclinedPayload(UUID bookingId, String reason) {}

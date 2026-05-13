package ma.naja7i.booking.infrastructure.messaging.payload;

import java.util.UUID;

public record BookingCancelledPayload(UUID bookingId, UUID cancelledBy, String reason) {}

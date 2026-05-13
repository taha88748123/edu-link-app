package ma.naja7i.booking.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

public record BookingConfirmedPayload(UUID bookingId, Instant confirmedAt) {}

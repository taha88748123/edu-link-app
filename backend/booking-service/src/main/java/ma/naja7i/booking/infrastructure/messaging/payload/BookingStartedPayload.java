package ma.naja7i.booking.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

public record BookingStartedPayload(UUID bookingId, Instant startAt) {}

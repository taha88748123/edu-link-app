package ma.naja7i.booking.infrastructure.messaging.payload;

import java.util.UUID;

public record BookingDisputedPayload(UUID disputeId, UUID bookingId, String cause) {}

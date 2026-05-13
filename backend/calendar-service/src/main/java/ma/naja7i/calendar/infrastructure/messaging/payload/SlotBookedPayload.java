package ma.naja7i.calendar.infrastructure.messaging.payload;

import java.util.UUID;

/** Payload of {@code naja7i.calendar.slot.booked.v1}. */
public record SlotBookedPayload(UUID slotId, UUID bookingId) {}

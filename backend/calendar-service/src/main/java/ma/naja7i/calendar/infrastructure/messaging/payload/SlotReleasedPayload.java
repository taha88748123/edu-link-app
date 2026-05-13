package ma.naja7i.calendar.infrastructure.messaging.payload;

import java.util.UUID;

/** Payload of {@code naja7i.calendar.slot.released.v1}. */
public record SlotReleasedPayload(UUID slotId, String reason) {

    public static final String REASON_RESERVATION_EXPIRED = "RESERVATION_EXPIRED";
    public static final String REASON_BOOKING_CANCELLED   = "BOOKING_CANCELLED";
    public static final String REASON_PROF_REJECTED       = "PROF_REJECTED";
    public static final String REASON_USER_SUSPENDED      = "USER_SUSPENDED";
    public static final String REASON_MANUAL              = "MANUAL";
}

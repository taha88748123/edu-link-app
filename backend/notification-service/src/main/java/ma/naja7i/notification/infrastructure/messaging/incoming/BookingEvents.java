package ma.naja7i.notification.infrastructure.messaging.incoming;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Consumer-side mirrors of the {@code booking-service} event payloads (Phase 2).
 */
public final class BookingEvents {

    private BookingEvents() {}

    /** {@code naja7i.booking.requested.v1} */
    public record BookingRequested(UUID bookingId, UUID studentId, UUID profId, UUID slotId,
                                   BigDecimal amountMad) {}

    /** {@code naja7i.booking.accepted.v1} */
    public record BookingAccepted(UUID bookingId, UUID studentId, UUID profId,
                                  java.time.Instant acceptedAt) {}

    /** {@code naja7i.booking.cancelled.v1} */
    public record BookingCancelled(UUID bookingId, UUID studentId, UUID profId,
                                   String cancelledBy, String reason) {}
}

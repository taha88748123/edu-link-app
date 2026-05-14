package ma.naja7i.video.infrastructure.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * Subset of {@code naja7i.booking.accepted.v1} we care about. Extra fields from
 * the producer are tolerated. We ALSO accept optional scheduledStart/scheduledEnd
 * / studentId / profId if the producer chooses to embed them; otherwise we
 * derive sensible defaults at consume-time (v1: now+5min, 60min duration).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingAcceptedConsumedPayload(
        UUID bookingId,
        UUID studentId,
        UUID profId,
        Instant acceptedAt,
        Instant scheduledStart,
        Instant scheduledEnd
) {
}

package ma.naja7i.shared.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Common envelope for every Kafka event in NAJA7I.
 * Serialised as JSON in v1; partition key is provided by the producer (not part of payload).
 */
public record DomainEvent<T>(
        UUID eventId,
        String eventType,        // "naja7i.booking.requested.v1"
        Instant occurredAt,
        String producer,         // "booking-service"
        UUID traceId,
        int schemaVersion,
        T payload
) {

    public static <T> DomainEvent<T> create(String eventType, String producer, T payload) {
        return new DomainEvent<>(
                UUID.randomUUID(),
                eventType,
                Instant.now(),
                producer,
                UUID.randomUUID(),
                1,
                payload
        );
    }
}

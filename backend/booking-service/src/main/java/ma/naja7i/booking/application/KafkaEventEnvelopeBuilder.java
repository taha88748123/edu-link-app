package ma.naja7i.booking.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ma.naja7i.booking.domain.OutboxEvent;
import ma.naja7i.shared.events.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Builds the {@link OutboxEvent} row for a domain event (during a business transaction)
 * and rebuilds the {@link DomainEvent} envelope when the poller publishes it.
 *
 * <p>Serialization model: each outbox row stores the JSON payload (raw business object).
 * The envelope ({@code DomainEvent}) is rebuilt at publish time so {@code occurredAt}
 * reflects the production decision rather than the persistence time.
 */
@Component
@RequiredArgsConstructor
public class KafkaEventEnvelopeBuilder {

    public static final String PRODUCER = "booking-service";

    private final ObjectMapper kafkaObjectMapper;

    /** Build an outbox row from a business payload. */
    public OutboxEvent toOutbox(UUID aggregateId, String eventType, String partitionKey, Object payload) {
        try {
            return OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .partitionKey(partitionKey)
                    .payload(kafkaObjectMapper.writeValueAsString(payload))
                    .build();
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Failed to serialise outbox payload for " + eventType, ex);
        }
    }

    /** Wrap a previously-serialised outbox payload back into a {@link DomainEvent} envelope. */
    public DomainEvent<Object> toDomainEvent(OutboxEvent row) {
        try {
            Object payload = kafkaObjectMapper.readValue(row.getPayload(), Object.class);
            return DomainEvent.create(row.getEventType(), PRODUCER, payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Failed to deserialise outbox payload for event " + row.getId(), ex);
        }
    }
}

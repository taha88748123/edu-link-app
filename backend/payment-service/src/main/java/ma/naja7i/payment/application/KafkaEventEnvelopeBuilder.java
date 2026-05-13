package ma.naja7i.payment.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.domain.OutboxEvent;
import ma.naja7i.shared.events.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Same role as in booking-service: serialises an outbound payload into a JSONB
 * outbox row inside a business transaction, and rebuilds the
 * {@link DomainEvent} envelope when the poller ships it to Kafka.
 */
@Component
@RequiredArgsConstructor
public class KafkaEventEnvelopeBuilder {

    public static final String PRODUCER = "payment-service";

    private final ObjectMapper kafkaObjectMapper;

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

package ma.naja7i.ai.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ma.naja7i.ai.config.AiProperties;
import ma.naja7i.ai.domain.OutboxEvent;
import ma.naja7i.shared.events.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Bridges domain payloads to the outbox table and reconstructs the
 * {@link DomainEvent} envelope when the poller ships rows to Kafka.
 *
 * <p>Same contract as the payment-service builder so admin dashboards can
 * rely on a uniform envelope across services.</p>
 */
@Component
@RequiredArgsConstructor
public class KafkaEventEnvelopeBuilder {

    private final ObjectMapper kafkaObjectMapper;
    private final AiProperties properties;

    public OutboxEvent toOutbox(UUID aggregateId,
                                String eventType,
                                String partitionKey,
                                Object payload) {
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
            return DomainEvent.create(row.getEventType(), properties.getProducer(), payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Failed to deserialise outbox payload for event " + row.getId(), ex);
        }
    }
}

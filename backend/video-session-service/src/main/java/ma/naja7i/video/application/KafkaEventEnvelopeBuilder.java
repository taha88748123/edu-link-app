package ma.naja7i.video.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ma.naja7i.shared.events.DomainEvent;
import ma.naja7i.video.domain.OutboxEvent;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Serialises payloads for the outbox row inside a business TX and rebuilds the
 * {@link DomainEvent} envelope when the poller ships it to Kafka. Mirrors the
 * pattern in payment-service / booking-service.
 */
@Component
@RequiredArgsConstructor
public class KafkaEventEnvelopeBuilder {

    public static final String PRODUCER = "video-session-service";

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

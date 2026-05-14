package ma.naja7i.chat.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.chat.config.ChatProperties;
import ma.naja7i.shared.events.DomainEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around {@link KafkaTemplate} that builds the {@link DomainEvent}
 * envelope and tolerates send failures (logs them — chat in v1 does NOT use the
 * outbox pattern; loss of an emit event on hard infra failure is acceptable because
 * the data of record is the Mongo write).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {

    private final KafkaTemplate<String, DomainEvent<?>> kafkaTemplate;
    private final ChatProperties props;

    public <T> void publish(String topic, String partitionKey, String eventType, T payload) {
        try {
            DomainEvent<T> envelope = DomainEvent.create(eventType, props.getProducer(), payload);
            kafkaTemplate.send(topic, partitionKey, envelope);
        } catch (Exception ex) {
            // Never let a Kafka failure bubble up to the HTTP handler.
            log.warn("Kafka publish failed: topic={} key={} cause={}",
                    topic, partitionKey, ex.getMessage());
        }
    }
}

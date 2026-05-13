package ma.naja7i.auth.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import ma.naja7i.shared.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String PRODUCER = "auth-service";

    private final KafkaTemplate<String, DomainEvent<?>> kafkaTemplate;

    public <T> void publish(String topic, String partitionKey, String eventType, T payload) {
        DomainEvent<T> event = DomainEvent.create(eventType, PRODUCER, payload);
        kafkaTemplate.send(topic, partitionKey, event)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("Kafka publish failed: topic={}, key={}, eventType={}", topic, partitionKey, eventType, ex);
                    } else if (log.isDebugEnabled()) {
                        log.debug("Kafka published: topic={}, key={}, eventType={}", topic, partitionKey, eventType);
                    }
                });
    }
}

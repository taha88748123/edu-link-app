package ma.naja7i.calendar.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import ma.naja7i.shared.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around {@link KafkaTemplate} that enforces the {@link DomainEvent}
 * envelope and never lets a publish failure crash the caller (errors are logged
 * and surfaced only to observability — same convention as auth-service).
 */
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String PRODUCER = "calendar-service";

    private final KafkaTemplate<String, DomainEvent<?>> kafkaTemplate;

    public <T> void publish(String topic, String partitionKey, String eventType, T payload) {
        DomainEvent<T> event = DomainEvent.create(eventType, PRODUCER, payload);
        kafkaTemplate.send(topic, partitionKey, event)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        // NB: never log prof_id in error context (PII rule); the key is opaque here.
                        log.error("Kafka publish failed: topic={} eventType={}", topic, eventType, ex);
                    } else if (log.isDebugEnabled()) {
                        log.debug("Kafka published: topic={} eventType={} eventId={}",
                                topic, eventType, event.eventId());
                    }
                });
    }
}

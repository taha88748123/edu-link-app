package ma.naja7i.profile.infrastructure.messaging;

import ma.naja7i.shared.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaPublisher.class);
    private static final String PRODUCER_NAME = "user-profile-service";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public <T> void publish(String topic, String partitionKey, T payload) {
        DomainEvent<T> event = DomainEvent.create(topic, PRODUCER_NAME, payload);
        kafkaTemplate.send(topic, partitionKey, event)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("Kafka publish failed [topic={}, key={}]: {}",
                                topic, partitionKey, ex.getMessage());
                    } else {
                        log.debug("Kafka published [topic={}, key={}, eventId={}]",
                                topic, partitionKey, event.eventId());
                    }
                });
    }
}

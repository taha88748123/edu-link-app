package ma.naja7i.notification.infrastructure.messaging;

import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.config.NotificationProperties;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.shared.events.DomainEvent;
import ma.naja7i.shared.events.EventTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes the observability events documented in {@code events-catalog.md}
 * (section {@code Domaine notification}). Partition key = {@code userId}.
 */
@Component
@Slf4j
public class KafkaPublisher {

    private final KafkaTemplate<String, DomainEvent<?>> kafka;
    private final NotificationProperties props;

    public KafkaPublisher(KafkaTemplate<String, DomainEvent<?>> kafka, NotificationProperties props) {
        this.kafka = kafka;
        this.props = props;
    }

    public void publishDelivered(Long notifId, UUID userId, Channel channel, String providerMessageId) {
        DeliveredPayload payload = new DeliveredPayload(notifId, userId, channel.name(), providerMessageId);
        DomainEvent<DeliveredPayload> event = DomainEvent.create(
                EventTopics.NOTIF_DELIVERED, props.getProducer(), payload);
        kafka.send(EventTopics.NOTIF_DELIVERED, userId.toString(), event);
        log.debug("Published {} for notifId={}", EventTopics.NOTIF_DELIVERED, notifId);
    }

    public void publishFailed(Long notifId, UUID userId, Channel channel, String error) {
        FailedPayload payload = new FailedPayload(notifId, userId, channel.name(), error);
        DomainEvent<FailedPayload> event = DomainEvent.create(
                EventTopics.NOTIF_FAILED, props.getProducer(), payload);
        kafka.send(EventTopics.NOTIF_FAILED, userId.toString(), event);
        log.debug("Published {} for notifId={}", EventTopics.NOTIF_FAILED, notifId);
    }

    public record DeliveredPayload(Long notifId, UUID userId, String channel, String providerMessageId) {}
    public record FailedPayload(Long notifId, UUID userId, String channel, String error) {}
}

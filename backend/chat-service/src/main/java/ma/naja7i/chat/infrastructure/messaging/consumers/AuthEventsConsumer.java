package ma.naja7i.chat.infrastructure.messaging.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.chat.application.ConversationService;
import ma.naja7i.chat.application.IdempotentEventGuard;
import ma.naja7i.chat.infrastructure.messaging.payload.UserSuspendedInboundPayload;
import ma.naja7i.shared.events.DomainEvent;
import ma.naja7i.shared.events.EventTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Closes all conversations involving a suspended user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventsConsumer {

    private final ConversationService conversationService;
    private final IdempotentEventGuard guard;
    private final ObjectMapper mapper;

    @KafkaListener(topics = EventTopics.AUTH_USER_SUSPENDED, containerFactory = "kafkaListenerContainerFactory")
    public void onUserSuspended(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (evt == null || !guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            UserSuspendedInboundPayload p =
                    mapper.convertValue(evt.payload(), UserSuspendedInboundPayload.class);
            int closed = conversationService.closeAllForUser(p.userId(), "USER_SUSPENDED");
            log.info("Closed {} conversation(s) on user.suspended (userId={})", closed, p.userId());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("AuthEventsConsumer#onUserSuspended failed", ex);
            ack.acknowledge();
        }
    }
}

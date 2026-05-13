package ma.naja7i.calendar.infrastructure.messaging.consumers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.naja7i.calendar.application.ProfStatusService;
import ma.naja7i.calendar.infrastructure.messaging.payload.UserSuspendedPayload;
import ma.naja7i.shared.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code naja7i.auth.user.suspended.v1}: when a prof is suspended, every
 * future slot of theirs is blocked. For non-prof users this is a no-op (no slots).
 */
@Component
public class AuthEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuthEventsConsumer.class);

    private final ObjectMapper mapper;
    private final ProfStatusService profStatusService;

    public AuthEventsConsumer(ObjectMapper mapper, ProfStatusService profStatusService) {
        this.mapper = mapper;
        this.profStatusService = profStatusService;
    }

    @KafkaListener(
            topics = "${naja7i.kafka.topics.auth-user-suspended:naja7i.auth.user.suspended.v1}",
            groupId = "${spring.kafka.consumer.group-id:calendar-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserSuspended(Object message, Acknowledgment ack) {
        try {
            DomainEvent<UserSuspendedPayload> event = mapper.convertValue(message,
                    new TypeReference<>() {});
            UserSuspendedPayload payload = event.payload();
            if (payload == null || payload.userId() == null) {
                log.warn("auth.user.suspended event missing userId — ignoring");
            } else {
                profStatusService.handleProfRejectedOrSuspended(
                        event.eventId(), event.eventType(), payload.userId());
            }
        } catch (Exception e) {
            log.error("Failed to handle auth.user.suspended event: {}", e.getMessage(), e);
        } finally {
            ack.acknowledge();
        }
    }
}

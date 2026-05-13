package ma.naja7i.calendar.infrastructure.messaging.consumers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.naja7i.calendar.application.ProfStatusService;
import ma.naja7i.calendar.infrastructure.messaging.payload.ProfRejectedPayload;
import ma.naja7i.calendar.infrastructure.messaging.payload.ProfValidatedPayload;
import ma.naja7i.shared.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code naja7i.profile.prof.validated.v1} (no-op stub, dedup-only) and
 * {@code naja7i.profile.prof.rejected.v1} (blocks future slots).
 */
@Component
public class ProfileEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProfileEventsConsumer.class);

    private final ObjectMapper mapper;
    private final ProfStatusService profStatusService;

    public ProfileEventsConsumer(ObjectMapper mapper, ProfStatusService profStatusService) {
        this.mapper = mapper;
        this.profStatusService = profStatusService;
    }

    @KafkaListener(
            topics = "${naja7i.kafka.topics.profile-prof-validated:naja7i.profile.prof.validated.v1}",
            groupId = "${spring.kafka.consumer.group-id:calendar-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onProfValidated(Object message, Acknowledgment ack) {
        try {
            DomainEvent<ProfValidatedPayload> event = mapper.convertValue(message,
                    new TypeReference<>() {});
            ProfValidatedPayload payload = event.payload();
            if (payload == null || payload.userId() == null) {
                log.warn("prof.validated event missing userId — ignoring");
            } else {
                profStatusService.handleProfValidated(
                        event.eventId(), event.eventType(), payload.userId());
            }
        } catch (Exception e) {
            // Never block the consumer on bad payloads; v2 routes these to a DLT.
            log.error("Failed to handle prof.validated event: {}", e.getMessage(), e);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = "${naja7i.kafka.topics.profile-prof-rejected:naja7i.profile.prof.rejected.v1}",
            groupId = "${spring.kafka.consumer.group-id:calendar-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onProfRejected(Object message, Acknowledgment ack) {
        try {
            DomainEvent<ProfRejectedPayload> event = mapper.convertValue(message,
                    new TypeReference<>() {});
            ProfRejectedPayload payload = event.payload();
            if (payload == null || payload.userId() == null) {
                log.warn("prof.rejected event missing userId — ignoring");
            } else {
                profStatusService.handleProfRejectedOrSuspended(
                        event.eventId(), event.eventType(), payload.userId());
            }
        } catch (Exception e) {
            log.error("Failed to handle prof.rejected event: {}", e.getMessage(), e);
        } finally {
            ack.acknowledge();
        }
    }
}

package ma.naja7i.notification.infrastructure.messaging.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.application.DispatchRequest;
import ma.naja7i.notification.application.IdempotencyGuard;
import ma.naja7i.notification.application.NotificationDispatcher;
import ma.naja7i.notification.config.NotificationProperties;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.TemplateKey;
import ma.naja7i.notification.infrastructure.messaging.incoming.ProfileEvents;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.events.DomainEvent;
import ma.naja7i.shared.events.EventTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes the profile-domain topics that result in user-facing notifications.
 *
 * <pre>
 *   profile.prof.submitted  → admin alert  (template {@code admin_new_prof_pending})
 *   profile.prof.evaluated  → notify prof  (best-effort — payload defines QCM result)
 *   profile.prof.validated  → notify prof  (template {@code prof_validated})
 *   profile.prof.rejected   → notify prof  (template {@code prof_rejected})
 * </pre>
 */
@Component
@Slf4j
public class ProfileEventsConsumer {

    private final NotificationDispatcher dispatcher;
    private final IdempotencyGuard guard;
    private final ObjectMapper mapper;
    private final NotificationProperties props;

    public ProfileEventsConsumer(NotificationDispatcher dispatcher,
                                 IdempotencyGuard guard,
                                 ObjectMapper mapper,
                                 NotificationProperties props) {
        this.dispatcher = dispatcher;
        this.guard = guard;
        this.mapper = mapper;
        this.props = props;
    }

    @KafkaListener(topics = EventTopics.PROFILE_PROF_SUBMITTED, containerFactory = "kafkaListenerContainerFactory")
    public void onProfSubmitted(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (!guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            ProfileEvents.ProfSubmitted p = EventPayloads.as(evt, ProfileEvents.ProfSubmitted.class, mapper);
            Map<String, Object> params = new HashMap<>();
            params.put("profName", p.displayName() == null ? p.userId().toString() : p.displayName());
            params.put("subjects", p.subjects() == null ? "" : String.join(", ", p.subjects()));
            dispatcher.dispatch(DispatchRequest.builder()
                    .userId(adminInboxId())
                    .role(Role.ADMIN)
                    .channels(List.of(Channel.valueOf(props.getAdminAlerts().getChannel())))
                    .templateKey(TemplateKey.ADMIN_NEW_PROF_PENDING)
                    .locale(props.getAdminAlerts().getLocale())
                    .params(params)
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("ProfileEventsConsumer#onProfSubmitted failed", ex);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = EventTopics.PROFILE_PROF_EVALUATED, containerFactory = "kafkaListenerContainerFactory")
    public void onProfEvaluated(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        // v1 : ai-service may not emit this yet — skipped silently if absent (handled by Kafka topic auto-create).
        try {
            DomainEvent<?> evt = record.value();
            if (!guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            ProfileEvents.ProfEvaluated p = EventPayloads.as(evt, ProfileEvents.ProfEvaluated.class, mapper);
            // No dedicated template in v1 — reuse prof_validated / prof_rejected templates based on decision.
            String template = "PASS".equalsIgnoreCase(p.decision())
                    ? TemplateKey.PROF_VALIDATED
                    : TemplateKey.PROF_REJECTED;
            Map<String, Object> params = new HashMap<>();
            params.put("reason", "QCM score=" + p.qcmScore());
            dispatcher.dispatch(DispatchRequest.builder()
                    .userId(p.userId())
                    .role(Role.PROF)
                    .templateKey(template)
                    .locale("fr")
                    .params(params)
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("ProfileEventsConsumer#onProfEvaluated failed", ex);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = EventTopics.PROFILE_PROF_VALIDATED, containerFactory = "kafkaListenerContainerFactory")
    public void onProfValidated(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (!guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            ProfileEvents.ProfValidated p = EventPayloads.as(evt, ProfileEvents.ProfValidated.class, mapper);
            dispatcher.dispatch(DispatchRequest.builder()
                    .userId(p.userId())
                    .role(Role.PROF)
                    .templateKey(TemplateKey.PROF_VALIDATED)
                    .locale("fr")
                    .params(Map.of())
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("ProfileEventsConsumer#onProfValidated failed", ex);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = EventTopics.PROFILE_PROF_REJECTED, containerFactory = "kafkaListenerContainerFactory")
    public void onProfRejected(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (!guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            ProfileEvents.ProfRejected p = EventPayloads.as(evt, ProfileEvents.ProfRejected.class, mapper);
            dispatcher.dispatch(DispatchRequest.builder()
                    .userId(p.userId())
                    .role(Role.PROF)
                    .templateKey(TemplateKey.PROF_REJECTED)
                    .locale("fr")
                    .params(Map.of("reason", p.reason() == null ? "" : p.reason()))
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("ProfileEventsConsumer#onProfRejected failed", ex);
            ack.acknowledge();
        }
    }

    /** Stable UUID derived from the admin-inbox key — keeps {@code notification_log.user_id} typed. */
    private UUID adminInboxId() {
        return UUID.nameUUIDFromBytes(props.getAdminAlerts().getInboxKey().getBytes(StandardCharsets.UTF_8));
    }
}

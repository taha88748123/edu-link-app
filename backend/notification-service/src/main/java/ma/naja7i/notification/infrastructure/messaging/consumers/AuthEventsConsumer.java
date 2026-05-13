package ma.naja7i.notification.infrastructure.messaging.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.application.DispatchRequest;
import ma.naja7i.notification.application.IdempotencyGuard;
import ma.naja7i.notification.application.NotificationDispatcher;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.TemplateKey;
import ma.naja7i.notification.infrastructure.messaging.incoming.AuthEvents;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.events.DomainEvent;
import ma.naja7i.shared.events.EventTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes the three {@code naja7i.auth.*} topics that the notification-service cares
 * about and maps them to the right (template, channel) pair.
 *
 * <pre>
 *   auth.otp.requested        → SMS or WA (based on event's "channel" field) — template {@code otp_code}
 *   auth.user.registered      → preferred channel — template {@code welcome_&lt;ROLE&gt;}
 *   auth.password.reset       → EMAIL + WA       — template {@code password_reset}
 * </pre>
 */
@Component
@Slf4j
public class AuthEventsConsumer {

    private final NotificationDispatcher dispatcher;
    private final IdempotencyGuard guard;
    private final ObjectMapper mapper;

    public AuthEventsConsumer(NotificationDispatcher dispatcher,
                              IdempotencyGuard guard,
                              ObjectMapper mapper) {
        this.dispatcher = dispatcher;
        this.guard = guard;
        this.mapper = mapper;
    }

    @KafkaListener(topics = EventTopics.AUTH_OTP_REQUESTED, containerFactory = "kafkaListenerContainerFactory")
    public void onOtpRequested(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (!guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            AuthEvents.OtpRequested payload = EventPayloads.as(evt, AuthEvents.OtpRequested.class, mapper);
            Channel channel = "WA".equalsIgnoreCase(payload.channel()) ? Channel.WA : Channel.SMS;
            Map<String, Object> params = new HashMap<>();
            params.put("code", payload.code());
            params.put("expiresInMinutes", expiresInMinutes(payload.expiresAt()));
            dispatcher.dispatch(DispatchRequest.builder()
                    .userId(payload.userId())
                    .channels(List.of(channel))
                    .templateKey(TemplateKey.OTP_CODE)
                    .locale("fr")
                    .params(params)
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("AuthEventsConsumer#onOtpRequested failed", ex);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = EventTopics.AUTH_USER_REGISTERED, containerFactory = "kafkaListenerContainerFactory")
    public void onUserRegistered(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (!guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            AuthEvents.UserRegistered p = EventPayloads.as(evt, AuthEvents.UserRegistered.class, mapper);
            Role role = parseRole(p.role());
            String template = role == Role.PROF ? TemplateKey.WELCOME_PROF : TemplateKey.WELCOME_STUDENT;
            dispatcher.dispatch(DispatchRequest.builder()
                    .userId(p.userId())
                    .role(role)
                    .templateKey(template)
                    .locale("fr")
                    .params(Map.of("email", p.email() == null ? "" : p.email()))
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("AuthEventsConsumer#onUserRegistered failed", ex);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = EventTopics.AUTH_PASSWORD_RESET, containerFactory = "kafkaListenerContainerFactory")
    public void onPasswordReset(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (!guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            AuthEvents.PasswordResetRequested p =
                    EventPayloads.as(evt, AuthEvents.PasswordResetRequested.class, mapper);
            Map<String, Object> params = new HashMap<>();
            params.put("resetLink", "https://naja7i.app/reset?token=" + p.resetToken());
            params.put("expiresInMinutes", expiresInMinutes(p.expiresAt()));
            dispatcher.dispatch(DispatchRequest.builder()
                    .userId(p.userId())
                    .channels(List.of(Channel.EMAIL, Channel.WA))
                    .templateKey(TemplateKey.PASSWORD_RESET)
                    .locale("fr")
                    .params(params)
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("AuthEventsConsumer#onPasswordReset failed", ex);
            ack.acknowledge();
        }
    }

    private static Role parseRole(String s) {
        if (s == null) return Role.STUDENT;
        try { return Role.valueOf(s); } catch (Exception e) { return Role.STUDENT; }
    }

    private static long expiresInMinutes(Instant expiresAt) {
        if (expiresAt == null) return 10;
        long min = Duration.between(Instant.now(), expiresAt).toMinutes();
        return Math.max(min, 1);
    }

    /** Exposed for test fixtures. */
    static UUID asUuid(Object o) {
        if (o == null) return null;
        if (o instanceof UUID u) return u;
        return UUID.fromString(o.toString());
    }
}

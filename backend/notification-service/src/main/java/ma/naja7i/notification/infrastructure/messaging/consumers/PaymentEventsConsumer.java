package ma.naja7i.notification.infrastructure.messaging.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.application.DispatchRequest;
import ma.naja7i.notification.application.IdempotencyGuard;
import ma.naja7i.notification.application.NotificationDispatcher;
import ma.naja7i.notification.config.NotificationProperties;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.TemplateKey;
import ma.naja7i.notification.infrastructure.messaging.incoming.PaymentEvents;
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
 * Payment-domain consumers.
 *
 * <pre>
 *   payment.topup.submitted    → admin alert      (admin_new_topup)
 *   payment.topup.decided      → notify student   (topup_decided)
 *   payment.payout.requested   → admin alert      (admin_new_topup — reused as v1 stand-in)
 * </pre>
 */
@Component
@Slf4j
public class PaymentEventsConsumer {

    private final NotificationDispatcher dispatcher;
    private final IdempotencyGuard guard;
    private final ObjectMapper mapper;
    private final NotificationProperties props;

    public PaymentEventsConsumer(NotificationDispatcher dispatcher,
                                 IdempotencyGuard guard,
                                 ObjectMapper mapper,
                                 NotificationProperties props) {
        this.dispatcher = dispatcher;
        this.guard = guard;
        this.mapper = mapper;
        this.props = props;
    }

    @KafkaListener(topics = EventTopics.PAYMENT_TOPUP_SUBMITTED, containerFactory = "kafkaListenerContainerFactory")
    public void onTopupSubmitted(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (!guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            PaymentEvents.TopupSubmitted p = EventPayloads.as(evt, PaymentEvents.TopupSubmitted.class, mapper);
            Map<String, Object> params = new HashMap<>();
            params.put("studentName", p.studentId().toString());
            params.put("amount", p.amountMad() == null ? "" : p.amountMad().toPlainString());
            dispatcher.dispatch(DispatchRequest.builder()
                    .userId(adminInboxId())
                    .role(Role.ADMIN)
                    .channels(List.of(Channel.valueOf(props.getAdminAlerts().getChannel())))
                    .templateKey(TemplateKey.ADMIN_NEW_TOPUP)
                    .locale(props.getAdminAlerts().getLocale())
                    .params(params)
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("PaymentEventsConsumer#onTopupSubmitted failed", ex);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = EventTopics.PAYMENT_TOPUP_DECIDED, containerFactory = "kafkaListenerContainerFactory")
    public void onTopupDecided(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (!guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            PaymentEvents.TopupDecided p = EventPayloads.as(evt, PaymentEvents.TopupDecided.class, mapper);
            Map<String, Object> params = new HashMap<>();
            params.put("status", p.decision() == null ? "" : p.decision());
            params.put("amount", p.approvedAmountMad() == null ? "" : p.approvedAmountMad().toPlainString());
            dispatcher.dispatch(DispatchRequest.builder()
                    .userId(p.studentId())
                    .role(Role.STUDENT)
                    .templateKey(TemplateKey.TOPUP_DECIDED)
                    .locale("fr")
                    .params(params)
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("PaymentEventsConsumer#onTopupDecided failed", ex);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = EventTopics.PAYMENT_PAYOUT_REQUESTED, containerFactory = "kafkaListenerContainerFactory")
    public void onPayoutRequested(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (!guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            PaymentEvents.PayoutRequested p = EventPayloads.as(evt, PaymentEvents.PayoutRequested.class, mapper);
            Map<String, Object> params = new HashMap<>();
            params.put("studentName", p.profId().toString()); // template reused — variable name kept stable
            params.put("amount", p.amountMad() == null ? "" : p.amountMad().toPlainString());
            dispatcher.dispatch(DispatchRequest.builder()
                    .userId(adminInboxId())
                    .role(Role.ADMIN)
                    .channels(List.of(Channel.valueOf(props.getAdminAlerts().getChannel())))
                    .templateKey(TemplateKey.ADMIN_NEW_TOPUP)
                    .locale(props.getAdminAlerts().getLocale())
                    .params(params)
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("PaymentEventsConsumer#onPayoutRequested failed", ex);
            ack.acknowledge();
        }
    }

    private UUID adminInboxId() {
        return UUID.nameUUIDFromBytes(props.getAdminAlerts().getInboxKey().getBytes(StandardCharsets.UTF_8));
    }
}

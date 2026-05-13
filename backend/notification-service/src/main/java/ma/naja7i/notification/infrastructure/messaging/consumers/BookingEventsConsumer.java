package ma.naja7i.notification.infrastructure.messaging.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.application.DispatchRequest;
import ma.naja7i.notification.application.IdempotencyGuard;
import ma.naja7i.notification.application.NotificationDispatcher;
import ma.naja7i.notification.domain.TemplateKey;
import ma.naja7i.notification.infrastructure.messaging.incoming.BookingEvents;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.events.DomainEvent;
import ma.naja7i.shared.events.EventTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Phase-2 consumers for the {@code naja7i.booking.*} domain. Stubbed in v1 :
 * payloads are forwarded through the dispatcher but reuse existing welcome /
 * validated templates because dedicated templates are out of scope.
 */
@Component
@Slf4j
public class BookingEventsConsumer {

    private final NotificationDispatcher dispatcher;
    private final IdempotencyGuard guard;
    private final ObjectMapper mapper;

    public BookingEventsConsumer(NotificationDispatcher dispatcher,
                                 IdempotencyGuard guard,
                                 ObjectMapper mapper) {
        this.dispatcher = dispatcher;
        this.guard = guard;
        this.mapper = mapper;
    }

    @KafkaListener(topics = EventTopics.BOOKING_REQUESTED, containerFactory = "kafkaListenerContainerFactory")
    public void onBookingRequested(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (!guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            BookingEvents.BookingRequested p = EventPayloads.as(evt, BookingEvents.BookingRequested.class, mapper);
            log.info("[Phase 2] booking.requested received bookingId={} profId={} — using welcome_PROF as stand-in",
                    p.bookingId(), p.profId());
            dispatcher.dispatch(DispatchRequest.builder()
                    .userId(p.profId())
                    .role(Role.PROF)
                    .templateKey(TemplateKey.WELCOME_PROF) // dedicated template ships with Phase 2
                    .locale("fr")
                    .params(Map.of("bookingId", p.bookingId() == null ? "" : p.bookingId().toString()))
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("BookingEventsConsumer#onBookingRequested failed", ex);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = EventTopics.BOOKING_ACCEPTED, containerFactory = "kafkaListenerContainerFactory")
    public void onBookingAccepted(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (!guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            BookingEvents.BookingAccepted p = EventPayloads.as(evt, BookingEvents.BookingAccepted.class, mapper);
            log.info("[Phase 2] booking.accepted bookingId={} studentId={}", p.bookingId(), p.studentId());
            dispatcher.dispatch(DispatchRequest.builder()
                    .userId(p.studentId())
                    .role(Role.STUDENT)
                    .templateKey(TemplateKey.WELCOME_STUDENT)
                    .locale("fr")
                    .params(Map.of("bookingId", p.bookingId() == null ? "" : p.bookingId().toString()))
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("BookingEventsConsumer#onBookingAccepted failed", ex);
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = EventTopics.BOOKING_CANCELLED, containerFactory = "kafkaListenerContainerFactory")
    public void onBookingCancelled(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (!guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            BookingEvents.BookingCancelled p = EventPayloads.as(evt, BookingEvents.BookingCancelled.class, mapper);
            // Notify the counter-party (whoever did NOT cancel)
            log.info("[Phase 2] booking.cancelled bookingId={} reason={}", p.bookingId(), p.reason());
            // best-effort : both sides get a templated stub
            dispatcher.dispatch(DispatchRequest.builder()
                    .userId(p.studentId())
                    .role(Role.STUDENT)
                    .templateKey(TemplateKey.PROF_REJECTED)
                    .locale("fr")
                    .params(Map.of("reason", p.reason() == null ? "" : p.reason()))
                    .build());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("BookingEventsConsumer#onBookingCancelled failed", ex);
            ack.acknowledge();
        }
    }
}

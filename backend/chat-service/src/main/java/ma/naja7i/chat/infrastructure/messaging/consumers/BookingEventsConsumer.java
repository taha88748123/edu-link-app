package ma.naja7i.chat.infrastructure.messaging.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.chat.application.ConversationService;
import ma.naja7i.chat.application.IdempotentEventGuard;
import ma.naja7i.chat.domain.ConversationStatus;
import ma.naja7i.chat.infrastructure.messaging.payload.BookingAcceptedInboundPayload;
import ma.naja7i.chat.infrastructure.messaging.payload.BookingRequestedInboundPayload;
import ma.naja7i.chat.infrastructure.messaging.payload.BookingTerminationInboundPayload;
import ma.naja7i.shared.events.DomainEvent;
import ma.naja7i.shared.events.EventTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Reacts to booking-lifecycle events :
 * <ul>
 *   <li>{@code booking.requested} : create the conversation in PRE_ACCEPT (idempotent).</li>
 *   <li>{@code booking.accepted}  : PRE_ACCEPT → POST_ACCEPT.</li>
 *   <li>{@code booking.declined / cancelled / confirmed} : CLOSED.</li>
 * </ul>
 * Each handler dedupes on {@code eventId} via {@link IdempotentEventGuard} BEFORE side effects.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventsConsumer {

    private final ConversationService conversationService;
    private final IdempotentEventGuard guard;
    private final ObjectMapper mapper;

    @KafkaListener(topics = EventTopics.BOOKING_REQUESTED, containerFactory = "kafkaListenerContainerFactory")
    public void onBookingRequested(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (evt == null || !guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            BookingRequestedInboundPayload p =
                    mapper.convertValue(evt.payload(), BookingRequestedInboundPayload.class);
            conversationService.createOrGet(p.bookingId(), p.profId(), p.studentId(),
                    ConversationStatus.PRE_ACCEPT);
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
            if (evt == null || !guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            BookingAcceptedInboundPayload p =
                    mapper.convertValue(evt.payload(), BookingAcceptedInboundPayload.class);
            conversationService.transitionToPostAccept(p.bookingId());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("BookingEventsConsumer#onBookingAccepted failed", ex);
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = {EventTopics.BOOKING_DECLINED, EventTopics.BOOKING_CANCELLED, EventTopics.BOOKING_CONFIRMED},
            containerFactory = "kafkaListenerContainerFactory")
    public void onBookingTerminated(ConsumerRecord<String, DomainEvent<?>> record, Acknowledgment ack) {
        try {
            DomainEvent<?> evt = record.value();
            if (evt == null || !guard.acquire(evt.eventId(), record.topic())) { ack.acknowledge(); return; }
            BookingTerminationInboundPayload p =
                    mapper.convertValue(evt.payload(), BookingTerminationInboundPayload.class);
            conversationService.closeByBookingId(p.bookingId(), record.topic());
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("BookingEventsConsumer#onBookingTerminated failed", ex);
            ack.acknowledge();
        }
    }
}

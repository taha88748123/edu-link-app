package ma.naja7i.video.infrastructure.messaging.consumers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ma.naja7i.shared.events.DomainEvent;
import ma.naja7i.video.application.IdempotentEventGuard;
import ma.naja7i.video.application.SessionService;
import ma.naja7i.video.infrastructure.messaging.payload.BookingAcceptedConsumedPayload;
import ma.naja7i.video.infrastructure.messaging.payload.BookingCancelledConsumedPayload;
import ma.naja7i.video.infrastructure.messaging.payload.BookingDeclinedConsumedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes the three booking topics that drive the video session lifecycle:
 * <ul>
 *   <li>{@code booking.accepted}  -> create a SCHEDULED session</li>
 *   <li>{@code booking.cancelled} -> mark SCHEDULED -> EXPIRED (no-op otherwise)</li>
 *   <li>{@code booking.declined}  -> same as cancelled</li>
 * </ul>
 *
 * <p>Every handler calls {@link IdempotentEventGuard#markProcessed} BEFORE any
 * side effect, so duplicate deliveries are rejected before any DB write. Ack
 * is always committed (otherwise the broker would redeliver and re-trigger the
 * dedup — wasted cycles).
 */
@Component
@RequiredArgsConstructor
public class BookingEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingEventsConsumer.class);

    private final ObjectMapper kafkaObjectMapper;
    private final IdempotentEventGuard guard;
    private final SessionService sessionService;

    @KafkaListener(
            topics = "${naja7i.kafka.topics.booking-accepted:naja7i.booking.accepted.v1}",
            groupId = "${spring.kafka.consumer.group-id:video-session-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onBookingAccepted(Object message, Acknowledgment ack) {
        try {
            DomainEvent<BookingAcceptedConsumedPayload> event = kafkaObjectMapper.convertValue(
                    message, new TypeReference<>() {});
            BookingAcceptedConsumedPayload p = event.payload();
            if (p == null || p.bookingId() == null) {
                log.warn("booking.accepted missing bookingId — ignoring");
                return;
            }
            if (!guard.markProcessed(event.eventId(), event.eventType())) {
                return;
            }
            sessionService.onBookingAccepted(p.bookingId(), p.studentId(), p.profId(),
                    p.scheduledStart(), p.scheduledEnd());
        } catch (Exception ex) {
            log.error("Failed to handle booking.accepted: {}", ex.getMessage(), ex);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = "${naja7i.kafka.topics.booking-cancelled:naja7i.booking.cancelled.v1}",
            groupId = "${spring.kafka.consumer.group-id:video-session-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onBookingCancelled(Object message, Acknowledgment ack) {
        try {
            DomainEvent<BookingCancelledConsumedPayload> event = kafkaObjectMapper.convertValue(
                    message, new TypeReference<>() {});
            BookingCancelledConsumedPayload p = event.payload();
            if (p == null || p.bookingId() == null) {
                log.warn("booking.cancelled missing bookingId — ignoring");
                return;
            }
            if (!guard.markProcessed(event.eventId(), event.eventType())) {
                return;
            }
            sessionService.onBookingTerminated(p.bookingId());
        } catch (Exception ex) {
            log.error("Failed to handle booking.cancelled: {}", ex.getMessage(), ex);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = "${naja7i.kafka.topics.booking-declined:naja7i.booking.declined.v1}",
            groupId = "${spring.kafka.consumer.group-id:video-session-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onBookingDeclined(Object message, Acknowledgment ack) {
        try {
            DomainEvent<BookingDeclinedConsumedPayload> event = kafkaObjectMapper.convertValue(
                    message, new TypeReference<>() {});
            BookingDeclinedConsumedPayload p = event.payload();
            if (p == null || p.bookingId() == null) {
                log.warn("booking.declined missing bookingId — ignoring");
                return;
            }
            if (!guard.markProcessed(event.eventId(), event.eventType())) {
                return;
            }
            sessionService.onBookingTerminated(p.bookingId());
        } catch (Exception ex) {
            log.error("Failed to handle booking.declined: {}", ex.getMessage(), ex);
        } finally {
            ack.acknowledge();
        }
    }
}

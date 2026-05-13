package ma.naja7i.payment.infrastructure.messaging.consumers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.application.EscrowService;
import ma.naja7i.payment.application.IdempotentEventGuard;
import ma.naja7i.payment.infrastructure.messaging.payload.BookingCancelledConsumedPayload;
import ma.naja7i.payment.infrastructure.messaging.payload.BookingConfirmedConsumedPayload;
import ma.naja7i.payment.infrastructure.messaging.payload.BookingDeclinedConsumedPayload;
import ma.naja7i.payment.infrastructure.messaging.payload.BookingRequestedConsumedPayload;
import ma.naja7i.payment.infrastructure.messaging.payload.DisputeResolvedConsumedPayload;
import ma.naja7i.shared.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Single-class hub that consumes the 4 booking topics that move money:
 * <ul>
 *   <li>{@code booking.requested}    -> lock escrow (debit student)</li>
 *   <li>{@code booking.confirmed}    -> release escrow to prof</li>
 *   <li>{@code booking.cancelled}    -> refund student</li>
 *   <li>{@code booking.declined}     -> refund student</li>
 *   <li>{@code booking.dispute_resolved} -> split / pay / refund per admin decision</li>
 * </ul>
 *
 * <p>Every handler is idempotent: the {@link IdempotentEventGuard} is called
 * BEFORE invoking the escrow service, so duplicate deliveries are rejected
 * before any side effect. Ack is always committed (otherwise the broker would
 * redeliver and re-trigger the dedup — wasted cycles).
 */
@Component
@RequiredArgsConstructor
public class BookingEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingEventsConsumer.class);

    private final ObjectMapper kafkaObjectMapper;
    private final IdempotentEventGuard guard;
    private final EscrowService escrowService;

    @KafkaListener(
            topics = "${naja7i.kafka.topics.booking-requested:naja7i.booking.requested.v1}",
            groupId = "${spring.kafka.consumer.group-id:payment-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onBookingRequested(Object message, Acknowledgment ack) {
        try {
            DomainEvent<BookingRequestedConsumedPayload> event = kafkaObjectMapper.convertValue(
                    message, new TypeReference<>() {});
            BookingRequestedConsumedPayload p = event.payload();
            if (p == null || p.bookingId() == null || p.studentId() == null) {
                log.warn("booking.requested missing critical fields — ignoring");
                return;
            }
            if (!guard.markProcessed(event.eventId(), event.eventType())) {
                return;
            }
            escrowService.onBookingRequested(p.bookingId(), p.studentId(), p.profId(),
                    p.amountMinor(), traceIdOf(event));
        } catch (Exception ex) {
            log.error("Failed to handle booking.requested: {}", ex.getMessage(), ex);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = "${naja7i.kafka.topics.booking-confirmed:naja7i.booking.confirmed.v1}",
            groupId = "${spring.kafka.consumer.group-id:payment-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onBookingConfirmed(Object message, Acknowledgment ack) {
        try {
            DomainEvent<BookingConfirmedConsumedPayload> event = kafkaObjectMapper.convertValue(
                    message, new TypeReference<>() {});
            BookingConfirmedConsumedPayload p = event.payload();
            if (p == null || p.bookingId() == null) {
                log.warn("booking.confirmed missing bookingId — ignoring");
                return;
            }
            if (!guard.markProcessed(event.eventId(), event.eventType())) {
                return;
            }
            escrowService.onBookingConfirmed(p.bookingId(), traceIdOf(event));
        } catch (Exception ex) {
            log.error("Failed to handle booking.confirmed: {}", ex.getMessage(), ex);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = "${naja7i.kafka.topics.booking-cancelled:naja7i.booking.cancelled.v1}",
            groupId = "${spring.kafka.consumer.group-id:payment-service}",
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
            escrowService.onBookingCancelled(p.bookingId(), traceIdOf(event), p.reason());
        } catch (Exception ex) {
            log.error("Failed to handle booking.cancelled: {}", ex.getMessage(), ex);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = "${naja7i.kafka.topics.booking-declined:naja7i.booking.declined.v1}",
            groupId = "${spring.kafka.consumer.group-id:payment-service}",
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
            escrowService.onBookingCancelled(p.bookingId(), traceIdOf(event),
                    p.reason() == null ? "DECLINED" : p.reason());
        } catch (Exception ex) {
            log.error("Failed to handle booking.declined: {}", ex.getMessage(), ex);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = "${naja7i.kafka.topics.booking-dispute-resolved:naja7i.booking.dispute_resolved.v1}",
            groupId = "${spring.kafka.consumer.group-id:payment-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onDisputeResolved(Object message, Acknowledgment ack) {
        try {
            DomainEvent<DisputeResolvedConsumedPayload> event = kafkaObjectMapper.convertValue(
                    message, new TypeReference<>() {});
            DisputeResolvedConsumedPayload p = event.payload();
            if (p == null || p.bookingId() == null || p.resolution() == null) {
                log.warn("dispute_resolved missing critical fields — ignoring");
                return;
            }
            if (!guard.markProcessed(event.eventId(), event.eventType())) {
                return;
            }
            escrowService.onDisputeResolved(p.bookingId(), p.resolution(),
                    p.profShareMinor(), p.studentRefundMinor(), traceIdOf(event));
        } catch (Exception ex) {
            log.error("Failed to handle dispute_resolved: {}", ex.getMessage(), ex);
        } finally {
            ack.acknowledge();
        }
    }

    private static UUID traceIdOf(DomainEvent<?> event) {
        return event == null ? null : event.traceId();
    }
}

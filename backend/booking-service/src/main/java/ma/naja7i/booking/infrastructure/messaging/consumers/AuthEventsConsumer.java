package ma.naja7i.booking.infrastructure.messaging.consumers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ma.naja7i.booking.application.BookingService;
import ma.naja7i.booking.application.IdempotentEventGuard;
import ma.naja7i.booking.infrastructure.messaging.payload.UserSuspendedPayload;
import ma.naja7i.shared.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code naja7i.auth.user.suspended.v1}: cancel all open bookings of the
 * suspended user (as student). Idempotent via {@link IdempotentEventGuard}.
 */
@Component
@RequiredArgsConstructor
public class AuthEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuthEventsConsumer.class);

    private final ObjectMapper kafkaObjectMapper;
    private final IdempotentEventGuard guard;
    private final BookingService bookingService;

    @KafkaListener(
            topics = "${naja7i.kafka.topics.auth-user-suspended:naja7i.auth.user.suspended.v1}",
            groupId = "${spring.kafka.consumer.group-id:booking-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserSuspended(Object message, Acknowledgment ack) {
        try {
            DomainEvent<UserSuspendedPayload> event = kafkaObjectMapper.convertValue(
                    message, new TypeReference<>() {});
            UserSuspendedPayload payload = event.payload();
            if (payload == null || payload.userId() == null) {
                log.warn("auth.user.suspended event missing userId — ignoring");
                return;
            }
            if (!guard.markProcessed(event.eventId(), event.eventType())) {
                return;
            }
            int cancelled = bookingService.cancelOpenBookingsOf(payload.userId());
            if (cancelled > 0) {
                log.info("Cancelled {} open booking(s) after user.suspended", cancelled);
            }
        } catch (Exception ex) {
            log.error("Failed to handle auth.user.suspended event: {}", ex.getMessage(), ex);
        } finally {
            ack.acknowledge();
        }
    }
}

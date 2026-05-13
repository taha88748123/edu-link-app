package ma.naja7i.booking.infrastructure.messaging.consumers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ma.naja7i.booking.application.BookingService;
import ma.naja7i.booking.application.IdempotentEventGuard;
import ma.naja7i.booking.infrastructure.messaging.payload.BookingEndedConsumedPayload;
import ma.naja7i.shared.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code naja7i.video.session.ended.v1} and transitions EN_COURS -> TERMINEE.
 *
 * <p>Idempotent via {@link IdempotentEventGuard}. We do not produce a
 * {@code naja7i.booking.ended.v1} here — that event belongs to video-service
 * (per the events catalog).
 */
@Component
@RequiredArgsConstructor
public class VideoEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(VideoEventsConsumer.class);

    private final ObjectMapper kafkaObjectMapper;
    private final IdempotentEventGuard guard;
    private final BookingService bookingService;

    @KafkaListener(
            topics = "${naja7i.kafka.topics.video-session-ended:naja7i.video.session.ended.v1}",
            groupId = "${spring.kafka.consumer.group-id:booking-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onSessionEnded(Object message, Acknowledgment ack) {
        try {
            DomainEvent<BookingEndedConsumedPayload> event = kafkaObjectMapper.convertValue(
                    message, new TypeReference<>() {});
            BookingEndedConsumedPayload payload = event.payload();
            if (payload == null || payload.bookingId() == null) {
                log.warn("video.session.ended event missing bookingId — ignoring");
                return;
            }
            if (!guard.markProcessed(event.eventId(), event.eventType())) {
                return;
            }
            long durationSec = payload.durationSec() == null ? 0L : payload.durationSec();
            bookingService.markEnded(payload.bookingId(), durationSec);
        } catch (Exception ex) {
            log.error("Failed to handle video.session.ended event: {}", ex.getMessage(), ex);
        } finally {
            ack.acknowledge();
        }
    }
}

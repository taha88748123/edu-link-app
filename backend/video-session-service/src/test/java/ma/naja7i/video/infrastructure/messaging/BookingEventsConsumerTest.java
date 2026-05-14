package ma.naja7i.video.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ma.naja7i.shared.events.DomainEvent;
import ma.naja7i.video.application.IdempotentEventGuard;
import ma.naja7i.video.application.SessionService;
import ma.naja7i.video.infrastructure.messaging.consumers.BookingEventsConsumer;
import ma.naja7i.video.infrastructure.messaging.payload.BookingAcceptedConsumedPayload;
import ma.naja7i.video.infrastructure.messaging.payload.BookingCancelledConsumedPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-level dedup + delegation checks for the consumer. We don't boot Kafka —
 * we hand the handler a constructed DomainEvent map directly, mirroring what
 * Spring Kafka would deliver after JSON deserialisation.
 */
@ExtendWith(MockitoExtension.class)
class BookingEventsConsumerTest {

    @Mock IdempotentEventGuard guard;
    @Mock SessionService sessionService;
    @Mock Acknowledgment ack;

    BookingEventsConsumer consumer;
    ObjectMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new BookingEventsConsumer(mapper, guard, sessionService);
    }

    private DomainEvent<BookingAcceptedConsumedPayload> acceptedEvent(UUID bookingId,
                                                                     UUID studentId,
                                                                     UUID profId) {
        Instant start = Instant.now().plusSeconds(60);
        BookingAcceptedConsumedPayload p = new BookingAcceptedConsumedPayload(
                bookingId, studentId, profId, Instant.now(),
                start, start.plusSeconds(3600));
        return DomainEvent.create("naja7i.booking.accepted.v1", "booking-service", p);
    }

    private DomainEvent<BookingCancelledConsumedPayload> cancelledEvent(UUID bookingId) {
        BookingCancelledConsumedPayload p = new BookingCancelledConsumedPayload(
                bookingId, UUID.randomUUID(), "STUDENT_CHANGED_MIND");
        return DomainEvent.create("naja7i.booking.cancelled.v1", "booking-service", p);
    }

    // ============================================================ booking.accepted

    @Test
    void onBookingAccepted_callsService_whenNotYetProcessed() {
        UUID bookingId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID profId    = UUID.randomUUID();
        DomainEvent<BookingAcceptedConsumedPayload> event = acceptedEvent(bookingId, studentId, profId);
        when(guard.markProcessed(eq(event.eventId()), any(String.class))).thenReturn(true);

        consumer.onBookingAccepted(mapper.convertValue(event, Object.class), ack);

        verify(sessionService).onBookingAccepted(eq(bookingId), eq(studentId), eq(profId),
                any(Instant.class), any(Instant.class));
        verify(ack).acknowledge();
    }

    @Test
    void onBookingAccepted_skipsService_whenAlreadyProcessed() {
        UUID bookingId = UUID.randomUUID();
        DomainEvent<BookingAcceptedConsumedPayload> event =
                acceptedEvent(bookingId, UUID.randomUUID(), UUID.randomUUID());
        when(guard.markProcessed(eq(event.eventId()), any(String.class))).thenReturn(false);

        consumer.onBookingAccepted(mapper.convertValue(event, Object.class), ack);

        verify(sessionService, never()).onBookingAccepted(any(), any(), any(), any(), any());
        verify(ack).acknowledge();
    }

    @Test
    void onBookingAccepted_missingBookingId_skipsAndAcks() {
        // Build a payload with everything null — must be ignored before the guard.
        BookingAcceptedConsumedPayload p = new BookingAcceptedConsumedPayload(
                null, null, null, null, null, null);
        DomainEvent<BookingAcceptedConsumedPayload> event = DomainEvent.create(
                "naja7i.booking.accepted.v1", "booking-service", p);

        consumer.onBookingAccepted(mapper.convertValue(event, Object.class), ack);

        verify(guard, never()).markProcessed(any(), any());
        verify(sessionService, never()).onBookingAccepted(any(), any(), any(), any(), any());
        verify(ack).acknowledge();
    }

    // ============================================================ booking.cancelled / declined

    @Test
    void onBookingCancelled_callsTerminator_whenNotYetProcessed() {
        UUID bookingId = UUID.randomUUID();
        DomainEvent<BookingCancelledConsumedPayload> event = cancelledEvent(bookingId);
        when(guard.markProcessed(eq(event.eventId()), any(String.class))).thenReturn(true);

        consumer.onBookingCancelled(mapper.convertValue(event, Object.class), ack);

        verify(sessionService, times(1)).onBookingTerminated(eq(bookingId));
        verify(ack).acknowledge();
    }

    @Test
    void onBookingCancelled_dedup_skipsService() {
        UUID bookingId = UUID.randomUUID();
        DomainEvent<BookingCancelledConsumedPayload> event = cancelledEvent(bookingId);
        when(guard.markProcessed(eq(event.eventId()), any(String.class))).thenReturn(false);

        consumer.onBookingCancelled(mapper.convertValue(event, Object.class), ack);

        verify(sessionService, never()).onBookingTerminated(any());
        verify(ack).acknowledge();
    }
}

package ma.naja7i.booking.application;

import ma.naja7i.booking.config.BookingProperties;
import ma.naja7i.booking.domain.Booking;
import ma.naja7i.booking.domain.OutboxEvent;
import ma.naja7i.booking.infrastructure.persistence.BookingRepository;
import ma.naja7i.booking.infrastructure.persistence.BookingStateTransitionRepository;
import ma.naja7i.booking.infrastructure.persistence.OutboxRepository;
import ma.naja7i.booking.infrastructure.rest.CalendarInternalClient;
import ma.naja7i.booking.infrastructure.rest.CalendarSlotView;
import ma.naja7i.booking.infrastructure.rest.PricingInternalClient;
import ma.naja7i.booking.infrastructure.rest.QuoteResult;
import ma.naja7i.booking.infrastructure.web.dto.CreateBookingRequest;
import ma.naja7i.shared.dto.BookingStatus;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.dto.SchoolLevel;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingStateTransitionRepository transitionRepository;
    @Mock OutboxRepository outboxRepository;
    @Mock KafkaEventEnvelopeBuilder envelopeBuilder;
    @Mock CalendarInternalClient calendarClient;
    @Mock PricingInternalClient pricingClient;

    BookingService service;
    BookingProperties properties;

    UUID studentId;
    UUID profId;
    UUID slotId;

    @BeforeEach
    void setup() {
        properties = new BookingProperties();
        // No wide stubs here — each test sets up only what it needs (UnnecessaryStubbingException trap).
        service = new BookingService(bookingRepository, transitionRepository,
                outboxRepository, envelopeBuilder, calendarClient, pricingClient, properties);
        studentId = UUID.randomUUID();
        profId    = UUID.randomUUID();
        slotId    = UUID.randomUUID();
    }

    private CreateBookingRequest req() {
        return new CreateBookingRequest(profId, slotId, "MATH",
                SchoolLevel.LYCEE, Modality.ONLINE, 60, null);
    }

    private QuoteResult quote() {
        return new QuoteResult(15000L, 1500L, 16500L, "MAD");
    }

    private CalendarSlotView reservedSlot() {
        Instant start = Instant.now().plusSeconds(3600);
        return new CalendarSlotView(slotId, profId, start, start.plusSeconds(3600),
                Modality.ONLINE, "RESERVED_PENDING", UUID.randomUUID(),
                Instant.now().plusSeconds(600));
    }

    private OutboxEvent dummyOutbox() {
        return OutboxEvent.builder().id(UUID.randomUUID()).build();
    }

    // ============================================================ create

    @Test
    void create_happyPath_persistsBookingAndOutboxRow() {
        when(pricingClient.quote(eq(profId), eq("MATH"), eq(SchoolLevel.LYCEE),
                eq(Modality.ONLINE), eq(60), any(), any())).thenReturn(quote());
        when(calendarClient.reserveSlot(eq(slotId), any(UUID.class))).thenReturn(reservedSlot());
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(envelopeBuilder.toOutbox(any(UUID.class), any(String.class), any(String.class), any()))
                .thenReturn(dummyOutbox());

        Booking saved = service.create(studentId, req());

        assertThat(saved.getStatus()).isEqualTo(BookingStatus.DEMANDEE);
        assertThat(saved.getStudentId()).isEqualTo(studentId);
        assertThat(saved.getProfId()).isEqualTo(profId);
        assertThat(saved.getAmountMinor()).isEqualTo(15000L);
        assertThat(saved.getPlatformFeeMinor()).isEqualTo(1500L);
        assertThat(saved.getTotalAmountMinor()).isEqualTo(16500L);
        verify(outboxRepository).save(any(OutboxEvent.class));
        verify(transitionRepository).save(any());
    }

    @Test
    void create_calendarReturnsConflict_releasesNothingAndPropagates() {
        when(pricingClient.quote(any(), any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(quote());
        when(calendarClient.reserveSlot(eq(slotId), any(UUID.class)))
                .thenThrow(new BusinessException(ErrorCode.BOOKING_SLOT_NOT_AVAILABLE, "boom"));

        assertThatThrownBy(() -> service.create(studentId, req()))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOKING_SLOT_NOT_AVAILABLE);

        // Reservation never succeeded -> nothing to release.
        verify(calendarClient, never()).releaseSlot(any(UUID.class), any());
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    // ============================================================ accept

    @Test
    void accept_validProf_transitionsToAccepted_andConfirmsSlot() {
        Booking b = Booking.builder()
                .id(UUID.randomUUID()).studentId(studentId).profId(profId).slotId(slotId)
                .status(BookingStatus.DEMANDEE)
                .scheduledStart(Instant.now().plusSeconds(3600))
                .scheduledEnd(Instant.now().plusSeconds(7200))
                .amountMinor(0L).platformFeeMinor(0L).totalAmountMinor(0L).currency("MAD")
                .build();
        when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(envelopeBuilder.toOutbox(any(UUID.class), any(String.class), any(String.class), any()))
                .thenReturn(dummyOutbox());

        Booking accepted = service.accept(b.getId(), profId);

        assertThat(accepted.getStatus()).isEqualTo(BookingStatus.ACCEPTEE_PAR_PROF);
        verify(calendarClient).confirmSlot(eq(slotId), eq(b.getId()));
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    void accept_otherProfsBooking_throwsForbidden() {
        Booking b = Booking.builder()
                .id(UUID.randomUUID()).studentId(studentId).profId(profId).slotId(slotId)
                .status(BookingStatus.DEMANDEE)
                .scheduledStart(Instant.now()).scheduledEnd(Instant.now().plusSeconds(60))
                .amountMinor(0L).platformFeeMinor(0L).totalAmountMinor(0L).currency("MAD")
                .build();
        when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.accept(b.getId(), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.AUTH_FORBIDDEN_ROLE);
    }

    @Test
    void accept_wrongStatus_throwsInvalidTransition() {
        Booking b = Booking.builder()
                .id(UUID.randomUUID()).studentId(studentId).profId(profId).slotId(slotId)
                .status(BookingStatus.ACCEPTEE_PAR_PROF)
                .scheduledStart(Instant.now()).scheduledEnd(Instant.now().plusSeconds(60))
                .amountMinor(0L).platformFeeMinor(0L).totalAmountMinor(0L).currency("MAD")
                .build();
        when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.accept(b.getId(), profId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOKING_INVALID_TRANSITION);
    }

    // ============================================================ cancel

    @Test
    void cancel_fromDemandee_transitionsAndReleasesSlot() {
        Booking b = Booking.builder()
                .id(UUID.randomUUID()).studentId(studentId).profId(profId).slotId(slotId)
                .status(BookingStatus.DEMANDEE)
                .scheduledStart(Instant.now().plusSeconds(3600))
                .scheduledEnd(Instant.now().plusSeconds(7200))
                .amountMinor(0L).platformFeeMinor(0L).totalAmountMinor(0L).currency("MAD")
                .build();
        when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(envelopeBuilder.toOutbox(any(UUID.class), any(String.class), any(String.class), any()))
                .thenReturn(dummyOutbox());

        Booking cancelled = service.cancel(b.getId(), studentId, Role.STUDENT, "changed mind");
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.ANNULEE);
        verify(calendarClient).releaseSlot(eq(slotId), any());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
    }

    @Test
    void cancel_fromTerminee_throwsInvalidTransition() {
        Booking b = Booking.builder()
                .id(UUID.randomUUID()).studentId(studentId).profId(profId).slotId(slotId)
                .status(BookingStatus.TERMINEE)
                .scheduledStart(Instant.now().minusSeconds(7200))
                .scheduledEnd(Instant.now().minusSeconds(3600))
                .amountMinor(0L).platformFeeMinor(0L).totalAmountMinor(0L).currency("MAD")
                .build();
        when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.cancel(b.getId(), studentId, Role.STUDENT, "late"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOKING_INVALID_TRANSITION);
    }

    @Test
    void cancel_afterScheduledStart_throwsInvalidTransition() {
        Booking b = Booking.builder()
                .id(UUID.randomUUID()).studentId(studentId).profId(profId).slotId(slotId)
                .status(BookingStatus.ACCEPTEE_PAR_PROF)
                .scheduledStart(Instant.now().minusSeconds(60))
                .scheduledEnd(Instant.now().plusSeconds(3600))
                .amountMinor(0L).platformFeeMinor(0L).totalAmountMinor(0L).currency("MAD")
                .build();
        when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.cancel(b.getId(), studentId, Role.STUDENT, "oops"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOKING_INVALID_TRANSITION);
    }

    // ============================================================ confirm

    @Test
    void confirm_fromTerminee_transitionsToConfirmed() {
        Booking b = Booking.builder()
                .id(UUID.randomUUID()).studentId(studentId).profId(profId).slotId(slotId)
                .status(BookingStatus.TERMINEE)
                .scheduledStart(Instant.now().minusSeconds(7200))
                .scheduledEnd(Instant.now().minusSeconds(3600))
                .amountMinor(0L).platformFeeMinor(0L).totalAmountMinor(0L).currency("MAD")
                .build();
        when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(envelopeBuilder.toOutbox(any(UUID.class), any(String.class), any(String.class), any()))
                .thenReturn(dummyOutbox());

        Booking confirmed = service.confirm(b.getId(), studentId);

        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CONFIRMEE);
        assertThat(confirmed.getConfirmedAt()).isNotNull();
    }

    @Test
    void confirm_byOtherStudent_throwsForbidden() {
        Booking b = Booking.builder()
                .id(UUID.randomUUID()).studentId(studentId).profId(profId).slotId(slotId)
                .status(BookingStatus.TERMINEE)
                .scheduledStart(Instant.now()).scheduledEnd(Instant.now().plusSeconds(60))
                .amountMinor(0L).platformFeeMinor(0L).totalAmountMinor(0L).currency("MAD")
                .build();
        when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.confirm(b.getId(), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.AUTH_FORBIDDEN_ROLE);
    }

    // ============================================================ start

    @Test
    void start_tooEarly_throwsInvalidTransition() {
        Booking b = Booking.builder()
                .id(UUID.randomUUID()).studentId(studentId).profId(profId).slotId(slotId)
                .status(BookingStatus.ACCEPTEE_PAR_PROF)
                // Scheduled in 2h — leadTolerance is 15min, so :start is forbidden now.
                .scheduledStart(Instant.now().plusSeconds(7200))
                .scheduledEnd(Instant.now().plusSeconds(10800))
                .amountMinor(0L).platformFeeMinor(0L).totalAmountMinor(0L).currency("MAD")
                .build();
        when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.start(b.getId(), profId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOKING_INVALID_TRANSITION);
    }

    @Test
    void start_withinTolerance_transitionsToInProgress() {
        Booking b = Booking.builder()
                .id(UUID.randomUUID()).studentId(studentId).profId(profId).slotId(slotId)
                .status(BookingStatus.ACCEPTEE_PAR_PROF)
                .scheduledStart(Instant.now().plusSeconds(60))  // within 15-min tolerance
                .scheduledEnd(Instant.now().plusSeconds(3600))
                .amountMinor(0L).platformFeeMinor(0L).totalAmountMinor(0L).currency("MAD")
                .build();
        when(bookingRepository.findById(b.getId())).thenReturn(Optional.of(b));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(envelopeBuilder.toOutbox(any(UUID.class), any(String.class), any(String.class), any()))
                .thenReturn(dummyOutbox());

        Booking started = service.start(b.getId(), profId);

        assertThat(started.getStatus()).isEqualTo(BookingStatus.EN_COURS);
        assertThat(started.getActualStart()).isNotNull();
    }
}

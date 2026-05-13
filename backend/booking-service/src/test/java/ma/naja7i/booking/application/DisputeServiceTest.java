package ma.naja7i.booking.application;

import ma.naja7i.booking.config.BookingProperties;
import ma.naja7i.booking.domain.Booking;
import ma.naja7i.booking.domain.Dispute;
import ma.naja7i.booking.infrastructure.persistence.BookingRepository;
import ma.naja7i.booking.infrastructure.persistence.BookingStateTransitionRepository;
import ma.naja7i.booking.infrastructure.persistence.DisputeRepository;
import ma.naja7i.shared.dto.BookingStatus;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock DisputeRepository disputeRepository;
    @Mock BookingRepository bookingRepository;
    @Mock BookingStateTransitionRepository transitionRepository;
    @Mock BookingService bookingService;

    BookingProperties properties;
    DisputeService service;

    UUID studentId;
    UUID profId;
    UUID slotId;
    UUID bookingId;

    @BeforeEach
    void setup() {
        properties = new BookingProperties(); // disputeWindowHours=24, leadToleranceMinutes=15
        service = new DisputeService(disputeRepository, bookingRepository,
                transitionRepository, bookingService, properties);
        studentId = UUID.randomUUID();
        profId    = UUID.randomUUID();
        slotId    = UUID.randomUUID();
        bookingId = UUID.randomUUID();
    }

    private Booking booking(BookingStatus status, Instant confirmedAt) {
        return Booking.builder()
                .id(bookingId).studentId(studentId).profId(profId).slotId(slotId)
                .status(status)
                .scheduledStart(Instant.now().minusSeconds(7200))
                .scheduledEnd(Instant.now().minusSeconds(3600))
                .confirmedAt(confirmedAt)
                .amountMinor(0L).platformFeeMinor(0L).totalAmountMinor(0L).currency("MAD")
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void openDispute_inEnCours_creates() {
        Booking b = booking(BookingStatus.EN_COURS, null);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));
        when(disputeRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> i.getArgument(0));

        Dispute d = service.openDispute(bookingId, studentId, Role.STUDENT, "Prof never connected", null);

        assertThat(d.getBookingId()).isEqualTo(bookingId);
        assertThat(b.getStatus()).isEqualTo(BookingStatus.LITIGE);
    }

    @Test
    void openDispute_inConfirmee_withinWindow_creates() {
        Instant confirmed = Instant.now().minus(Duration.ofHours(2)); // within 24h
        Booking b = booking(BookingStatus.CONFIRMEE, confirmed);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));
        when(disputeRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(i -> i.getArgument(0));

        Dispute d = service.openDispute(bookingId, studentId, Role.STUDENT, "audio issue", null);

        assertThat(d.getBookingId()).isEqualTo(bookingId);
    }

    @Test
    void openDispute_inConfirmee_beyondWindow_throws() {
        Instant confirmed = Instant.now().minus(Duration.ofHours(48)); // beyond 24h
        Booking b = booking(BookingStatus.CONFIRMEE, confirmed);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.openDispute(bookingId, studentId,
                Role.STUDENT, "late dispute", null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOKING_INVALID_TRANSITION);
    }

    @Test
    void openDispute_inDemandee_throws() {
        Booking b = booking(BookingStatus.DEMANDEE, null);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.openDispute(bookingId, studentId,
                Role.STUDENT, "too early", null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.BOOKING_INVALID_TRANSITION);
    }

    @Test
    void openDispute_byOtherStudent_throwsForbidden() {
        Booking b = booking(BookingStatus.EN_COURS, null);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(b));
        // findByBookingId may or may not be called depending on order; use lenient
        lenient().when(disputeRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.openDispute(bookingId, UUID.randomUUID(),
                Role.STUDENT, "not me", null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.AUTH_FORBIDDEN_ROLE);
    }

}

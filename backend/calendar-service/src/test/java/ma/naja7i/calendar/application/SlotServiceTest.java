package ma.naja7i.calendar.application;

import ma.naja7i.calendar.config.CalendarProperties;
import ma.naja7i.calendar.domain.Slot;
import ma.naja7i.calendar.domain.SlotStatus;
import ma.naja7i.calendar.infrastructure.messaging.KafkaEventPublisher;
import ma.naja7i.calendar.infrastructure.persistence.SlotRepository;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {

    @Mock SlotRepository slotRepository;
    @Mock KafkaEventPublisher publisher;

    SlotService slotService;

    private UUID profId;
    private UUID slotId;
    private UUID bookingId;
    private CalendarProperties props;

    @BeforeEach
    void setup() {
        profId = UUID.randomUUID();
        slotId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        props = new CalendarProperties();
        props.setTtlMinutes(10);
        slotService = new SlotService(slotRepository, publisher, props);
    }

    private Slot available() {
        return Slot.builder()
                .id(slotId).profId(profId)
                .startAt(Instant.now().plusSeconds(7200))
                .endAt(Instant.now().plusSeconds(7200 + 3600))
                .modality(Modality.ONLINE)
                .status(SlotStatus.AVAILABLE)
                .version(0)
                .build();
    }

    // ---------------------------------------------------------------- reserve

    @Test
    void reserve_available_transitionsToReservedPending_andSetsTtl() {
        Slot s = available();
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(s));
        when(slotRepository.saveAndFlush(any(Slot.class))).thenAnswer(inv -> inv.getArgument(0));

        Slot reserved = slotService.reserve(slotId, bookingId);

        assertThat(reserved.getStatus()).isEqualTo(SlotStatus.RESERVED_PENDING);
        assertThat(reserved.getBookingId()).isEqualTo(bookingId);
        assertThat(reserved.getReservedUntil()).isAfter(Instant.now());
        assertThat(reserved.getReservedUntil())
                .isBefore(Instant.now().plusSeconds((props.getTtlMinutes() * 60L) + 5));
        verify(publisher, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    void reserve_alreadyBooked_throwsConflict() {
        Slot s = available();
        s.setStatus(SlotStatus.BOOKED);
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> slotService.reserve(slotId, bookingId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CALENDAR_SLOT_CONFLICT);
    }

    @Test
    void reserve_blocked_throwsConflict() {
        Slot s = available();
        s.setStatus(SlotStatus.BLOCKED);
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> slotService.reserve(slotId, bookingId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CALENDAR_SLOT_CONFLICT);
    }

    @Test
    void reserve_optimisticLockingFailure_translatesToConflict() {
        Slot s = available();
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(s));
        when(slotRepository.saveAndFlush(any(Slot.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Slot.class, slotId));

        assertThatThrownBy(() -> slotService.reserve(slotId, bookingId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CALENDAR_SLOT_CONFLICT);
    }

    // ---------------------------------------------------------------- confirm

    @Test
    void confirm_reservedPending_transitionsToBooked_andEmitsEvent() {
        Slot s = available();
        s.setStatus(SlotStatus.RESERVED_PENDING);
        s.setBookingId(bookingId);
        s.setReservedUntil(Instant.now().plusSeconds(60));
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(s));
        when(slotRepository.saveAndFlush(any(Slot.class))).thenAnswer(inv -> inv.getArgument(0));

        Slot confirmed = slotService.confirm(slotId, bookingId);
        assertThat(confirmed.getStatus()).isEqualTo(SlotStatus.BOOKED);
        assertThat(confirmed.getReservedUntil()).isNull();
        verify(publisher).publish(
                eq(EventTopics.CALENDAR_SLOT_BOOKED),
                eq(profId.toString()),
                eq(EventTopics.CALENDAR_SLOT_BOOKED),
                any());
    }

    @Test
    void confirm_notReservedPending_throwsConflict() {
        Slot s = available();
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> slotService.confirm(slotId, bookingId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CALENDAR_SLOT_CONFLICT);
    }

    @Test
    void confirm_wrongBookingId_throwsConflict() {
        Slot s = available();
        s.setStatus(SlotStatus.RESERVED_PENDING);
        s.setBookingId(bookingId);
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> slotService.confirm(slotId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CALENDAR_SLOT_CONFLICT);
    }

    // ---------------------------------------------------------------- release

    @Test
    void release_fromReservedPending_emitsReleasedEvent() {
        Slot s = available();
        s.setStatus(SlotStatus.RESERVED_PENDING);
        s.setBookingId(bookingId);
        s.setReservedUntil(Instant.now().minusSeconds(1)); // already expired
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(s));
        when(slotRepository.saveAndFlush(any(Slot.class))).thenAnswer(inv -> inv.getArgument(0));

        Slot released = slotService.release(slotId, "RESERVATION_EXPIRED");
        assertThat(released.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
        assertThat(released.getBookingId()).isNull();
        assertThat(released.getReservedUntil()).isNull();
        verify(publisher).publish(
                eq(EventTopics.CALENDAR_SLOT_RELEASED),
                eq(profId.toString()),
                eq(EventTopics.CALENDAR_SLOT_RELEASED),
                any());
    }

    @Test
    void release_fromAvailable_throwsConflict() {
        Slot s = available();
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> slotService.release(slotId, null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CALENDAR_SLOT_CONFLICT);
    }

    // ---------------------------------------------------------------- block / unblock

    @Test
    void block_available_transitionsToBlocked() {
        Slot s = available();
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(s));
        when(slotRepository.save(any(Slot.class))).thenAnswer(inv -> inv.getArgument(0));

        Slot blocked = slotService.block(profId, slotId);
        assertThat(blocked.getStatus()).isEqualTo(SlotStatus.BLOCKED);
    }

    @Test
    void unblock_blocked_transitionsToAvailable_andEmitsCreatedEvent() {
        Slot s = available();
        s.setStatus(SlotStatus.BLOCKED);
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(s));
        when(slotRepository.save(any(Slot.class))).thenAnswer(inv -> inv.getArgument(0));

        Slot unblocked = slotService.unblock(profId, slotId);
        assertThat(unblocked.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
        verify(publisher).publish(
                eq(EventTopics.CALENDAR_SLOT_CREATED),
                eq(profId.toString()),
                eq(EventTopics.CALENDAR_SLOT_CREATED),
                any());
    }

    @Test
    void block_otherProfsSlot_throwsNotFound() {
        Slot s = available();
        when(slotRepository.findById(slotId)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> slotService.block(UUID.randomUUID(), slotId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CALENDAR_SLOT_NOT_FOUND);
    }
}

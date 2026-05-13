package ma.naja7i.calendar.application;

import ma.naja7i.calendar.domain.AvailabilityRule;
import ma.naja7i.calendar.domain.Recurrence;
import ma.naja7i.calendar.domain.Slot;
import ma.naja7i.calendar.domain.SlotStatus;
import ma.naja7i.calendar.infrastructure.persistence.AvailabilityRuleRepository;
import ma.naja7i.calendar.infrastructure.persistence.SlotRepository;
import ma.naja7i.calendar.infrastructure.web.dto.CreateAvailabilityRuleRequest;
import ma.naja7i.calendar.infrastructure.web.dto.UpdateAvailabilityRuleRequest;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityRuleServiceTest {

    @Mock AvailabilityRuleRepository ruleRepository;
    @Mock SlotRepository slotRepository;

    @InjectMocks
    AvailabilityRuleService service;

    private UUID profId;

    @BeforeEach
    void setup() {
        profId = UUID.randomUUID();
    }

    // ---------------------------------------------------------------- WEEKLY happy path

    @Test
    void create_weekly_happyPath_persistsRule() {
        CreateAvailabilityRuleRequest req = new CreateAvailabilityRuleRequest(
                Recurrence.WEEKLY, (short) 1, null,
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                "Africa/Casablanca");
        when(ruleRepository.save(any(AvailabilityRule.class))).thenAnswer(inv -> inv.getArgument(0));

        AvailabilityRule rule = service.create(profId, req);

        assertThat(rule.getRecurrence()).isEqualTo(Recurrence.WEEKLY);
        assertThat(rule.getDayOfWeek()).isEqualTo((short) 1);
        assertThat(rule.getDate()).isNull();
        assertThat(rule.isActive()).isTrue();
        verify(ruleRepository).save(any(AvailabilityRule.class));
    }

    // ---------------------------------------------------------------- ONE_OFF happy path

    @Test
    void create_oneOff_happyPath_persistsRule() {
        CreateAvailabilityRuleRequest req = new CreateAvailabilityRuleRequest(
                Recurrence.ONE_OFF, null, LocalDate.now().plusDays(3),
                LocalTime.of(14, 0), LocalTime.of(16, 0),
                "Africa/Casablanca");
        when(ruleRepository.save(any(AvailabilityRule.class))).thenAnswer(inv -> inv.getArgument(0));

        AvailabilityRule rule = service.create(profId, req);

        assertThat(rule.getRecurrence()).isEqualTo(Recurrence.ONE_OFF);
        assertThat(rule.getDate()).isNotNull();
        assertThat(rule.getDayOfWeek()).isNull();
    }

    // ---------------------------------------------------------------- validation matrix

    @Test
    void create_rejects_endBeforeStart() {
        CreateAvailabilityRuleRequest req = new CreateAvailabilityRuleRequest(
                Recurrence.WEEKLY, (short) 1, null,
                LocalTime.of(12, 0), LocalTime.of(9, 0),
                "Africa/Casablanca");

        assertThatThrownBy(() -> service.create(profId, req))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void create_rejects_weekly_withoutDayOfWeek() {
        CreateAvailabilityRuleRequest req = new CreateAvailabilityRuleRequest(
                Recurrence.WEEKLY, null, null,
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                "Africa/Casablanca");

        assertThatThrownBy(() -> service.create(profId, req))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void create_rejects_oneOff_inThePast() {
        CreateAvailabilityRuleRequest req = new CreateAvailabilityRuleRequest(
                Recurrence.ONE_OFF, null, LocalDate.now().minusDays(1),
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                "Africa/Casablanca");

        assertThatThrownBy(() -> service.create(profId, req))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void create_rejects_unknownTimezone() {
        CreateAvailabilityRuleRequest req = new CreateAvailabilityRuleRequest(
                Recurrence.WEEKLY, (short) 1, null,
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                "Atlantis/UnknownLand");

        assertThatThrownBy(() -> service.create(profId, req))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.CALENDAR_TIMEZONE_INVALID);
    }

    // ---------------------------------------------------------------- soft delete

    @Test
    void softDelete_blocksFutureAvailableSlotsFromRule() {
        UUID ruleId = UUID.randomUUID();
        AvailabilityRule rule = AvailabilityRule.builder()
                .id(ruleId).profId(profId)
                .recurrence(Recurrence.WEEKLY).dayOfWeek((short) 1)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0))
                .timezone("Africa/Casablanca").active(true)
                .build();
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        Slot s1 = Slot.builder()
                .id(UUID.randomUUID()).profId(profId)
                .startAt(Instant.now().plusSeconds(86400))
                .endAt(Instant.now().plusSeconds(86400 + 3600))
                .modality(Modality.ONLINE).status(SlotStatus.AVAILABLE)
                .generatedFromRuleId(ruleId).version(0)
                .build();
        when(slotRepository.findByGeneratedFromRuleIdAndStatusAndStartAtGreaterThan(
                eq(ruleId), eq(SlotStatus.AVAILABLE), any())).thenReturn(List.of(s1));

        int blocked = service.softDelete(profId, ruleId);

        assertThat(blocked).isEqualTo(1);
        assertThat(rule.isActive()).isFalse();
        assertThat(s1.getStatus()).isEqualTo(SlotStatus.BLOCKED);
        verify(ruleRepository, times(1)).save(rule);
    }

    @Test
    void update_swappingRecurrence_clearsOppositeField() {
        UUID ruleId = UUID.randomUUID();
        AvailabilityRule rule = AvailabilityRule.builder()
                .id(ruleId).profId(profId)
                .recurrence(Recurrence.WEEKLY).dayOfWeek((short) 1)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0))
                .timezone("Africa/Casablanca").active(true)
                .build();
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any(AvailabilityRule.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateAvailabilityRuleRequest req = new UpdateAvailabilityRuleRequest(
                Recurrence.ONE_OFF, null, LocalDate.now().plusDays(7),
                null, null, null, null);

        AvailabilityRule updated = service.update(profId, ruleId, req);
        assertThat(updated.getRecurrence()).isEqualTo(Recurrence.ONE_OFF);
        assertThat(updated.getDayOfWeek()).isNull();
        assertThat(updated.getDate()).isEqualTo(LocalDate.now().plusDays(7));
    }
}

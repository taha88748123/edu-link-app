package ma.naja7i.calendar.application;

import ma.naja7i.calendar.config.CalendarProperties;
import ma.naja7i.calendar.domain.AvailabilityRule;
import ma.naja7i.calendar.domain.Recurrence;
import ma.naja7i.calendar.domain.Slot;
import ma.naja7i.calendar.domain.SlotStatus;
import ma.naja7i.calendar.infrastructure.messaging.KafkaEventPublisher;
import ma.naja7i.calendar.infrastructure.persistence.AvailabilityRuleRepository;
import ma.naja7i.calendar.infrastructure.persistence.SlotRepository;
import ma.naja7i.shared.dto.Modality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotMaterializerTest {

    @Mock AvailabilityRuleRepository ruleRepository;
    @Mock SlotRepository slotRepository;
    @Mock KafkaEventPublisher publisher;

    SlotMaterializer materializer;

    private UUID profId;
    private CalendarProperties props;

    @BeforeEach
    void setup() {
        profId = UUID.randomUUID();
        props = new CalendarProperties();
        props.setGenerationWindowDays(14); // small to keep test fast
        props.setDefaultSlotDurationMinutes(60);
        props.setDefaultModality(Modality.ONLINE);
        materializer = new SlotMaterializer(ruleRepository, slotRepository, publisher, props);
    }

    @Test
    void generate_oneOff_creates_consecutiveHourSlots() {
        UUID ruleId = UUID.randomUUID();
        LocalDate target = LocalDate.now().plusDays(2);
        AvailabilityRule rule = AvailabilityRule.builder()
                .id(ruleId).profId(profId)
                .recurrence(Recurrence.ONE_OFF)
                .date(target)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .timezone("UTC")
                .active(true)
                .build();
        when(ruleRepository.findByProfIdAndActiveTrue(profId)).thenReturn(List.of(rule));
        when(slotRepository.findByProfIdAndStartAt(any(), any())).thenReturn(Optional.empty());
        when(slotRepository.save(any(Slot.class))).thenAnswer(inv -> inv.getArgument(0));

        SlotMaterializer.Stats stats = materializer.generateFor(profId);

        // 9-10, 10-11, 11-12 → 3 hour-long slots
        assertThat(stats.created()).isEqualTo(3);
        assertThat(stats.skipped()).isZero();
        verify(slotRepository, times(3)).save(any(Slot.class));
        verify(publisher, times(3)).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    void generate_isIdempotent_skipsExisting() {
        UUID ruleId = UUID.randomUUID();
        LocalDate target = LocalDate.now().plusDays(2);
        AvailabilityRule rule = AvailabilityRule.builder()
                .id(ruleId).profId(profId)
                .recurrence(Recurrence.ONE_OFF)
                .date(target)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .timezone("UTC")
                .active(true)
                .build();
        when(ruleRepository.findByProfIdAndActiveTrue(profId)).thenReturn(List.of(rule));

        // First pass: nothing existing yet → save through, remember the start instants.
        Map<Instant, Slot> store = new HashMap<>();
        when(slotRepository.findByProfIdAndStartAt(any(), any()))
                .thenAnswer(inv -> Optional.ofNullable(store.get((Instant) inv.getArgument(1))));
        when(slotRepository.save(any(Slot.class))).thenAnswer(inv -> {
            Slot s = inv.getArgument(0);
            store.put(s.getStartAt(), s);
            return s;
        });

        SlotMaterializer.Stats first = materializer.generateFor(profId);
        assertThat(first.created()).isEqualTo(2);
        assertThat(first.skipped()).isZero();

        // Second pass: everything is already there → skipped.
        SlotMaterializer.Stats second = materializer.generateFor(profId);
        assertThat(second.created()).isZero();
        assertThat(second.skipped()).isEqualTo(2);
    }

    @Test
    void generate_noActiveRules_returnsZero() {
        when(ruleRepository.findByProfIdAndActiveTrue(profId)).thenReturn(List.of());
        SlotMaterializer.Stats stats = materializer.generateFor(profId);
        assertThat(stats.created()).isZero();
        assertThat(stats.skipped()).isZero();
        verify(slotRepository, times(0)).save(any());
    }

    @Test
    void generate_oneOff_outsideHorizon_isSkipped() {
        UUID ruleId = UUID.randomUUID();
        AvailabilityRule rule = AvailabilityRule.builder()
                .id(ruleId).profId(profId)
                .recurrence(Recurrence.ONE_OFF)
                .date(LocalDate.now().plusDays(props.getGenerationWindowDays() + 5))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .timezone("UTC")
                .active(true)
                .build();
        when(ruleRepository.findByProfIdAndActiveTrue(profId)).thenReturn(List.of(rule));

        SlotMaterializer.Stats stats = materializer.generateFor(profId);
        assertThat(stats.created()).isZero();
    }

    @Test
    void generate_assignsDefaultModality_andAvailableStatus() {
        UUID ruleId = UUID.randomUUID();
        AvailabilityRule rule = AvailabilityRule.builder()
                .id(ruleId).profId(profId)
                .recurrence(Recurrence.ONE_OFF)
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .timezone("UTC")
                .active(true)
                .build();
        when(ruleRepository.findByProfIdAndActiveTrue(profId)).thenReturn(List.of(rule));
        when(slotRepository.findByProfIdAndStartAt(any(), any())).thenReturn(Optional.empty());
        when(slotRepository.save(any(Slot.class))).thenAnswer(inv -> {
            Slot s = inv.getArgument(0);
            assertThat(s.getModality()).isEqualTo(Modality.ONLINE);
            assertThat(s.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
            assertThat(s.getGeneratedFromRuleId()).isEqualTo(ruleId);
            return s;
        });

        SlotMaterializer.Stats stats = materializer.generateFor(profId);
        assertThat(stats.created()).isEqualTo(1);
    }
}

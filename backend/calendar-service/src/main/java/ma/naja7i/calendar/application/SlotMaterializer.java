package ma.naja7i.calendar.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.calendar.config.CalendarProperties;
import ma.naja7i.calendar.domain.AvailabilityRule;
import ma.naja7i.calendar.domain.Recurrence;
import ma.naja7i.calendar.domain.Slot;
import ma.naja7i.calendar.domain.SlotStatus;
import ma.naja7i.calendar.infrastructure.messaging.KafkaEventPublisher;
import ma.naja7i.calendar.infrastructure.messaging.payload.SlotCreatedPayload;
import ma.naja7i.calendar.infrastructure.persistence.AvailabilityRuleRepository;
import ma.naja7i.calendar.infrastructure.persistence.SlotRepository;
import ma.naja7i.shared.events.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Turns {@link AvailabilityRule}s into concrete {@link Slot}s on the
 * {@code generation-window-days} sliding window.
 *
 * <p>Idempotent: a slot is skipped when {@code (prof_id, start_at)} already exists,
 * which means re-running {@code /slots:generate} is always safe.
 */
@Service
@RequiredArgsConstructor
public class SlotMaterializer {

    private static final Logger log = LoggerFactory.getLogger(SlotMaterializer.class);

    private final AvailabilityRuleRepository ruleRepository;
    private final SlotRepository slotRepository;
    private final KafkaEventPublisher publisher;
    private final CalendarProperties props;

    public record Stats(int created, int skipped) {}

    @Transactional
    public Stats generateFor(UUID profId) {
        List<AvailabilityRule> rules = ruleRepository.findByProfIdAndActiveTrue(profId);
        return generate(profId, rules);
    }

    Stats generate(UUID profId, List<AvailabilityRule> rules) {
        int created = 0;
        int skipped = 0;
        if (rules.isEmpty()) {
            log.debug("No active rules to materialize for traceCtx (profId hashed)={}", Integer.toHexString(profId.hashCode()));
            return new Stats(0, 0);
        }
        Instant horizonStart = Instant.now();
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate horizonEnd = today.plusDays(props.getGenerationWindowDays());

        List<Slot> toCreate = new ArrayList<>();
        for (AvailabilityRule rule : rules) {
            ZoneId zone = ZoneId.of(rule.getTimezone());
            switch (rule.getRecurrence()) {
                case WEEKLY -> {
                    short targetDow = rule.getDayOfWeek();
                    LocalDate cursor = today;
                    while (!cursor.isAfter(horizonEnd)) {
                        // Java DayOfWeek: 1=Mon ... 7=Sun → match domain 0=Mon ... 6=Sun
                        int domainDow = cursor.getDayOfWeek().getValue() - 1;
                        if (domainDow == targetDow) {
                            toCreate.addAll(materialiseDay(rule, cursor, zone, horizonStart));
                        }
                        cursor = cursor.plusDays(1);
                    }
                }
                case ONE_OFF -> {
                    LocalDate d = rule.getDate();
                    if (d != null && !d.isBefore(today) && !d.isAfter(horizonEnd)) {
                        toCreate.addAll(materialiseDay(rule, d, zone, horizonStart));
                    }
                }
            }
        }

        for (Slot s : toCreate) {
            if (slotRepository.findByProfIdAndStartAt(s.getProfId(), s.getStartAt()).isPresent()) {
                skipped++;
                continue;
            }
            Slot saved = slotRepository.save(s);
            created++;
            publisher.publish(
                    EventTopics.CALENDAR_SLOT_CREATED,
                    saved.getProfId().toString(),
                    EventTopics.CALENDAR_SLOT_CREATED,
                    new SlotCreatedPayload(saved.getId(), saved.getProfId(),
                            saved.getStartAt(), saved.getEndAt(), saved.getModality()));
        }
        log.info("Slot materialization: created={} skipped={}", created, skipped);
        return new Stats(created, skipped);
    }

    private List<Slot> materialiseDay(AvailabilityRule rule, LocalDate day,
                                      ZoneId zone, Instant horizonStart) {
        Duration slotDuration = Duration.ofMinutes(props.getDefaultSlotDurationMinutes());
        Instant start = LocalDateTime.of(day, rule.getStartTime()).atZone(zone).toInstant();
        Instant ruleEnd = LocalDateTime.of(day, rule.getEndTime()).atZone(zone).toInstant();
        List<Slot> out = new ArrayList<>();
        while (!start.plus(slotDuration).isAfter(ruleEnd)) {
            Instant end = start.plus(slotDuration);
            if (end.isAfter(horizonStart)) { // skip slots already in the past
                out.add(Slot.builder()
                        .id(UUID.randomUUID())
                        .profId(rule.getProfId())
                        .startAt(start)
                        .endAt(end)
                        .modality(props.getDefaultModality())
                        .status(SlotStatus.AVAILABLE)
                        .generatedFromRuleId(rule.getId())
                        .version(0)
                        .build());
            }
            start = end;
        }
        return out;
    }
}

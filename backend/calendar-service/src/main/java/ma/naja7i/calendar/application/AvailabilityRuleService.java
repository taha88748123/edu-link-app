package ma.naja7i.calendar.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.calendar.domain.AvailabilityRule;
import ma.naja7i.calendar.domain.Recurrence;
import ma.naja7i.calendar.domain.Slot;
import ma.naja7i.calendar.domain.SlotStatus;
import ma.naja7i.calendar.infrastructure.persistence.AvailabilityRuleRepository;
import ma.naja7i.calendar.infrastructure.persistence.SlotRepository;
import ma.naja7i.calendar.infrastructure.web.dto.CreateAvailabilityRuleRequest;
import ma.naja7i.calendar.infrastructure.web.dto.UpdateAvailabilityRuleRequest;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages {@link AvailabilityRule} CRUD on behalf of a PROF.
 *
 * <p>Cross-field validation (WEEKLY ⇄ dayOfWeek, ONE_OFF ⇄ future date, start &lt; end,
 * timezone supported by JVM) is centralised in {@link #validate(Recurrence, Short, LocalDate, LocalTime, LocalTime, String)}.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityRuleService {

    private final AvailabilityRuleRepository ruleRepository;
    private final SlotRepository slotRepository;

    @Transactional
    public AvailabilityRule create(UUID profId, CreateAvailabilityRuleRequest req) {
        String tz = req.timezone() == null ? "Africa/Casablanca" : req.timezone();
        validate(req.recurrence(), req.dayOfWeek(), req.date(), req.startTime(), req.endTime(), tz);
        AvailabilityRule rule = AvailabilityRule.builder()
                .id(UUID.randomUUID())
                .profId(profId)
                .recurrence(req.recurrence())
                .dayOfWeek(req.recurrence() == Recurrence.WEEKLY ? req.dayOfWeek() : null)
                .date(req.recurrence() == Recurrence.ONE_OFF ? req.date() : null)
                .startTime(req.startTime())
                .endTime(req.endTime())
                .timezone(tz)
                .active(true)
                .build();
        return ruleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityRule> listOwn(UUID profId) {
        return ruleRepository.findByProfIdOrderByCreatedAtDesc(profId);
    }

    @Transactional
    public AvailabilityRule update(UUID profId, UUID ruleId, UpdateAvailabilityRuleRequest req) {
        AvailabilityRule rule = loadOwned(profId, ruleId);
        Recurrence rec = req.recurrence()  != null ? req.recurrence()  : rule.getRecurrence();
        Short dow      = req.dayOfWeek()   != null ? req.dayOfWeek()   : rule.getDayOfWeek();
        LocalDate dt   = req.date()        != null ? req.date()        : rule.getDate();
        LocalTime st   = req.startTime()   != null ? req.startTime()   : rule.getStartTime();
        LocalTime en   = req.endTime()     != null ? req.endTime()     : rule.getEndTime();
        String tz      = req.timezone()    != null ? req.timezone()    : rule.getTimezone();

        // If switching recurrence kind, the unused field is cleared to keep the DB check happy.
        if (rec == Recurrence.WEEKLY) {
            dt = null;
        } else {
            dow = null;
        }

        validate(rec, dow, dt, st, en, tz);
        rule.setRecurrence(rec);
        rule.setDayOfWeek(dow);
        rule.setDate(dt);
        rule.setStartTime(st);
        rule.setEndTime(en);
        rule.setTimezone(tz);
        if (req.active() != null) {
            rule.setActive(req.active());
        }
        return ruleRepository.save(rule);
    }

    /**
     * Soft delete: marks the rule inactive and blocks every future AVAILABLE slot
     * generated from it. Slots already in RESERVED_PENDING / BOOKED are untouched —
     * the booking flow remains in charge of those.
     */
    @Transactional
    public int softDelete(UUID profId, UUID ruleId) {
        AvailabilityRule rule = loadOwned(profId, ruleId);
        rule.setActive(false);
        ruleRepository.save(rule);
        List<Slot> future = slotRepository
                .findByGeneratedFromRuleIdAndStatusAndStartAtGreaterThan(
                        ruleId, SlotStatus.AVAILABLE, Instant.now());
        for (Slot s : future) {
            s.setStatus(SlotStatus.BLOCKED);
        }
        return future.size();
    }

    private AvailabilityRule loadOwned(UUID profId, UUID ruleId) {
        AvailabilityRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Rule not found", Map.of("ruleId", ruleId)));
        if (!rule.getProfId().equals(profId)) {
            // Treat as not-found to avoid IDOR enumeration leaks.
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Rule not found", Map.of("ruleId", ruleId));
        }
        return rule;
    }

    /** Package-private so unit tests can exercise the validation matrix directly. */
    void validate(Recurrence recurrence, Short dayOfWeek, LocalDate date,
                  LocalTime start, LocalTime end, String timezone) {
        if (recurrence == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "recurrence required");
        }
        if (start == null || end == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "start/end time required");
        }
        if (!start.isBefore(end)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "startTime must be strictly before endTime");
        }
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CALENDAR_TIMEZONE_INVALID,
                    "Unknown timezone: " + timezone);
        }
        switch (recurrence) {
            case WEEKLY -> {
                if (dayOfWeek == null || dayOfWeek < 0 || dayOfWeek > 6) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                            "WEEKLY rule requires dayOfWeek in [0..6]");
                }
            }
            case ONE_OFF -> {
                if (date == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                            "ONE_OFF rule requires a date");
                }
                if (!date.isAfter(LocalDate.now(ZoneId.of(timezone)).minusDays(1))) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                            "ONE_OFF date must be today or in the future");
                }
            }
        }
    }
}

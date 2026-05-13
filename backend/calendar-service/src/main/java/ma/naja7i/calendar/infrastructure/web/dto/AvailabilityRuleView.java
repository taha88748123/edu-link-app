package ma.naja7i.calendar.infrastructure.web.dto;

import ma.naja7i.calendar.domain.AvailabilityRule;
import ma.naja7i.calendar.domain.Recurrence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityRuleView(
        UUID id,
        UUID profId,
        Recurrence recurrence,
        Short dayOfWeek,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String timezone,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static AvailabilityRuleView of(AvailabilityRule r) {
        return new AvailabilityRuleView(
                r.getId(), r.getProfId(), r.getRecurrence(),
                r.getDayOfWeek(), r.getDate(),
                r.getStartTime(), r.getEndTime(),
                r.getTimezone(), r.isActive(),
                r.getCreatedAt(), r.getUpdatedAt());
    }
}

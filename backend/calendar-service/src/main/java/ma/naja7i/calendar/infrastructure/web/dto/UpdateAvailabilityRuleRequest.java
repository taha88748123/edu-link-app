package ma.naja7i.calendar.infrastructure.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import ma.naja7i.calendar.domain.Recurrence;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateAvailabilityRuleRequest(
        Recurrence recurrence,
        @Min(0) @Max(6) Short dayOfWeek,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String timezone,
        Boolean active
) {}

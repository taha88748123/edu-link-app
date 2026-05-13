package ma.naja7i.calendar.infrastructure.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ma.naja7i.calendar.domain.Recurrence;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request body for {@code POST /api/v1/calendar/availability-rules}.
 *
 * <p>Validation beyond bean-validation (e.g. WEEKLY/ONE_OFF cross-field rules) is
 * performed in the service layer.
 */
public record CreateAvailabilityRuleRequest(
        @NotNull Recurrence recurrence,
        @Min(0) @Max(6) Short dayOfWeek,
        LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String timezone
) {}

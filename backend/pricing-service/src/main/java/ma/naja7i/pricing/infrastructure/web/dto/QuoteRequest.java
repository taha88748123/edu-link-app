package ma.naja7i.pricing.infrastructure.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;

import java.util.UUID;

/**
 * Body for {@code POST /api/v1/pricing/quote} and the internal twin.
 *
 * <ul>
 *   <li>If {@code packId} is set, the pack overrides any individual rate;
 *       the unit price = {@code total_amount_minor / sessions_count}.</li>
 *   <li>Otherwise we look up the best matching {@code prof_pricing} row
 *       (exact day match first, then {@code day_of_week IS NULL}).</li>
 * </ul>
 */
public record QuoteRequest(
        @NotNull UUID profId,
        @NotBlank String subjectCode,
        @NotNull SchoolLevel level,
        @NotNull Modality modality,
        @NotNull @Min(1) @Max(480) Integer durationMinutes,
        @Min(0) @Max(6) Integer dayOfWeek,
        UUID packId
) {
}

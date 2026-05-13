package ma.naja7i.pricing.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;

/**
 * Same shape as {@link CreateRateRequest} — used for full-resource update (PUT).
 * {@code active} lets the prof temporarily disable a rate without deleting it.
 */
public record UpdateRateRequest(
        @NotBlank String subjectCode,
        @NotNull SchoolLevel level,
        @NotNull Modality modality,
        @NotNull @Min(1) @Max(480) Integer durationMinutes,
        @Min(0) @Max(6) Integer dayOfWeek,
        @NotBlank @Pattern(regexp = "\\d+(\\.\\d{1,2})?",
                message = "must be a decimal with up to 2 decimals")
        @DecimalMin(value = "0.00", inclusive = true) String amountMad,
        Boolean active
) {
}

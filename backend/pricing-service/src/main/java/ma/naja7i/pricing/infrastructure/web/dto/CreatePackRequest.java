package ma.naja7i.pricing.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;

import java.math.BigDecimal;

/**
 * Body for creating a session pack.
 *
 * @param totalAmountMad decimal string for the whole pack (not per session).
 * @param discountPct    informational discount applied to compute {@code totalAmountMad}; 0..100.
 */
public record CreatePackRequest(
        @NotBlank String subjectCode,
        @NotNull SchoolLevel level,
        @NotNull Modality modality,
        @NotNull @Min(1) @Max(100) Integer sessionsCount,
        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        @DecimalMax(value = "100.00", inclusive = true)
        BigDecimal discountPct,
        @NotBlank @Pattern(regexp = "\\d+(\\.\\d{1,2})?",
                message = "must be a decimal with up to 2 decimals")
        @DecimalMin(value = "0.00", inclusive = true) String totalAmountMad
) {
}

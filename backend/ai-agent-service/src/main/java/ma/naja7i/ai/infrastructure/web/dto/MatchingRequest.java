package ma.naja7i.ai.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Request body for {@code POST /api/v1/ai/matching/profs}. The student is
 * identified by the gateway-injected headers (never duplicated in the
 * body), so the request only carries the filters used to score profs.
 *
 * <p>Optional fields ({@code budgetMaxMad}, {@code city}, {@code dayOfWeek},
 * {@code durationMinutes}, {@code limit}) influence ranking but do not
 * exclude candidates by themselves.</p>
 */
public record MatchingRequest(
        @NotBlank String subjectCode,
        @NotBlank String level,
        @NotBlank String modality,
        BigDecimal budgetMaxMad,
        String city,
        Integer dayOfWeek,
        @Min(15) Integer durationMinutes,
        @Min(1) Integer limit
) {
}

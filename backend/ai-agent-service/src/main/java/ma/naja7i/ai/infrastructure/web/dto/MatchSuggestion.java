package ma.naja7i.ai.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One scored prof suggestion returned to the student. Pricing fields are
 * optional because v1 may not always be able to resolve the prof's avg
 * price from features alone.
 */
public record MatchSuggestion(
        UUID profId,
        double score,
        String rationale,
        BigDecimal basePriceMad,
        BigDecimal totalPriceMad
) {
}

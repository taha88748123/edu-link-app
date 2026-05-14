package ma.naja7i.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ma.naja7i.ai.domain.MatchingFeatures;
import ma.naja7i.ai.infrastructure.web.dto.MatchingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure-function scorer used by the matching pipeline. The formula is
 * intentionally simple and deterministic so v1 ranks profs without any
 * LLM call:
 *
 * <pre>
 *   base   = 0.5
 *   +0.2   if rating >= 4.0
 *   +0.1   if rating >= 4.5
 *   +0.1   if prof.city matches request.city (case-insensitive)
 *   +0.1   if prof.avg_price_minor is within 80% of request.budget_max
 *   cap at 1.0
 * </pre>
 *
 * <p>Missing JSONB fields are treated as "no bonus" — the function never
 * throws on malformed feature blobs (the worst case is a 0.5 base
 * score).</p>
 */
@Component
@RequiredArgsConstructor
public class MatchScoreCalculator {

    private static final Logger log = LoggerFactory.getLogger(MatchScoreCalculator.class);

    /** Base score awarded to any candidate that already passed the hard filters. */
    public static final double BASE = 0.5;
    public static final double BONUS_RATING_4   = 0.2;
    public static final double BONUS_RATING_45  = 0.1;
    public static final double BONUS_CITY       = 0.1;
    public static final double BONUS_BUDGET     = 0.1;
    public static final double MAX_SCORE        = 1.0;

    /** A prof's price counts as "within budget" once it sits at or below this ratio. */
    static final BigDecimal BUDGET_THRESHOLD_RATIO = new BigDecimal("0.80");

    private final ObjectMapper kafkaObjectMapper;

    /**
     * Computes the score of a single prof against the student's request.
     * Always returns a value in {@code [0.0, 1.0]}.
     */
    public double score(MatchingFeatures prof, MatchingRequest request) {
        double s = BASE;
        JsonNode node = parse(prof);
        if (node != null) {
            double rating = node.path("rating").asDouble(0.0);
            if (rating >= 4.0) s += BONUS_RATING_4;
            if (rating >= 4.5) s += BONUS_RATING_45;

            if (request != null && request.city() != null && !request.city().isBlank()) {
                String profCity = node.path("city").asText("");
                if (profCity.equalsIgnoreCase(request.city().trim())) {
                    s += BONUS_CITY;
                }
            }

            if (request != null && request.budgetMaxMad() != null) {
                double avgPriceMinor = node.path("avg_price_minor").asDouble(0.0);
                if (avgPriceMinor > 0.0) {
                    BigDecimal priceMad = BigDecimal.valueOf(avgPriceMinor)
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    BigDecimal threshold = request.budgetMaxMad()
                            .multiply(BUDGET_THRESHOLD_RATIO);
                    if (priceMad.compareTo(threshold) <= 0) {
                        s += BONUS_BUDGET;
                    }
                }
            }
        }
        if (s > MAX_SCORE) s = MAX_SCORE;
        if (s < 0.0) s = 0.0;
        return s;
    }

    private JsonNode parse(MatchingFeatures prof) {
        if (prof == null || prof.getFeatures() == null) return null;
        try {
            return kafkaObjectMapper.readTree(prof.getFeatures());
        } catch (Exception ex) {
            log.debug("Malformed prof feature blob for user {}: {}",
                    prof.getUserId(), ex.getMessage());
            return null;
        }
    }
}

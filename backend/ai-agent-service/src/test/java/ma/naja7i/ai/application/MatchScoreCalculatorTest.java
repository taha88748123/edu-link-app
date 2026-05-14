package ma.naja7i.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.naja7i.ai.domain.MatchingFeatures;
import ma.naja7i.ai.infrastructure.web.dto.MatchingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for the deterministic scoring formula. Verifies the base
 * value, each individual bonus, and the upper bound at 1.0.
 */
class MatchScoreCalculatorTest {

    MatchScoreCalculator calculator;

    @BeforeEach
    void setup() {
        calculator = new MatchScoreCalculator(new ObjectMapper());
    }

    private MatchingFeatures prof(String featuresJson) {
        return MatchingFeatures.builder()
                .userId(UUID.randomUUID())
                .role("PROF")
                .features(featuresJson)
                .version(0)
                .build();
    }

    private MatchingRequest request(String city, BigDecimal budget) {
        return new MatchingRequest("MATH", "LYCEE", "ONLINE", budget, city, null, null, null);
    }

    @Test
    void baselineScore_isExactlyHalf_whenNoBonuses() {
        MatchingFeatures p = prof("{\"subjects\":[\"MATH\"],\"levels\":[\"LYCEE\"],\"modalities\":[\"ONLINE\"]}");
        double s = calculator.score(p, request(null, null));
        assertThat(s).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void rating4_addsBonus_butNot45() {
        MatchingFeatures p = prof("{\"rating\":4.0}");
        double s = calculator.score(p, request(null, null));
        assertThat(s).isCloseTo(0.5 + MatchScoreCalculator.BONUS_RATING_4, within(1e-9));
    }

    @Test
    void rating45_addsBothBonuses() {
        MatchingFeatures p = prof("{\"rating\":4.5}");
        double s = calculator.score(p, request(null, null));
        assertThat(s).isCloseTo(
                0.5 + MatchScoreCalculator.BONUS_RATING_4 + MatchScoreCalculator.BONUS_RATING_45,
                within(1e-9));
    }

    @Test
    void cityMatch_addsBonus_caseInsensitive() {
        MatchingFeatures p = prof("{\"city\":\"Casablanca\"}");
        double s = calculator.score(p, request("casablanca", null));
        assertThat(s).isCloseTo(0.5 + MatchScoreCalculator.BONUS_CITY, within(1e-9));
    }

    @Test
    void cityMismatch_addsNoBonus() {
        MatchingFeatures p = prof("{\"city\":\"Casablanca\"}");
        double s = calculator.score(p, request("Rabat", null));
        assertThat(s).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void budgetWithin80Percent_addsBonus() {
        // budget = 200 MAD, threshold = 160 MAD = 16000 minor.
        // prof avg_price_minor = 15000 (150 MAD) -> below threshold -> bonus.
        MatchingFeatures p = prof("{\"avg_price_minor\":15000}");
        double s = calculator.score(p, request(null, new BigDecimal("200")));
        assertThat(s).isCloseTo(0.5 + MatchScoreCalculator.BONUS_BUDGET, within(1e-9));
    }

    @Test
    void budgetOverThreshold_addsNoBonus() {
        // budget = 200 MAD, threshold = 160 MAD. prof = 200 MAD -> no bonus.
        MatchingFeatures p = prof("{\"avg_price_minor\":20000}");
        double s = calculator.score(p, request(null, new BigDecimal("200")));
        assertThat(s).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void perfectMatch_isCappedAtOne() {
        MatchingFeatures p = prof("{\"rating\":4.9,\"city\":\"Casablanca\",\"avg_price_minor\":1000}");
        double s = calculator.score(p, request("Casablanca", new BigDecimal("200")));
        assertThat(s).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void malformedFeatures_returnsBaseScoreOnly() {
        MatchingFeatures p = prof("not json at all");
        double s = calculator.score(p, request("Casa", new BigDecimal("200")));
        assertThat(s).isCloseTo(0.5, within(1e-9));
    }
}

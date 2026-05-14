package ma.naja7i.ai.application;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Computes the USD cost of an Anthropic call. Pricing constants follow the
 * Opus tier: $15 per million input tokens, $75 per million output tokens.
 * The result is always stored as {@link BigDecimal} (6 decimals) to avoid
 * float drift in cost dashboards.
 */
@Component
public class AnthropicCostCalculator {

    /** Cost per million input tokens in USD (Opus tier). */
    public static final BigDecimal INPUT_PRICE_PER_MILLION  = new BigDecimal("15");

    /** Cost per million output tokens in USD (Opus tier). */
    public static final BigDecimal OUTPUT_PRICE_PER_MILLION = new BigDecimal("75");

    private static final BigDecimal MILLION = new BigDecimal("1000000");

    public BigDecimal compute(int tokensIn, int tokensOut) {
        BigDecimal in  = BigDecimal.valueOf(Math.max(0, tokensIn));
        BigDecimal out = BigDecimal.valueOf(Math.max(0, tokensOut));
        BigDecimal cost = in.multiply(INPUT_PRICE_PER_MILLION)
                .add(out.multiply(OUTPUT_PRICE_PER_MILLION))
                .divide(MILLION, 6, RoundingMode.HALF_UP);
        return cost;
    }
}

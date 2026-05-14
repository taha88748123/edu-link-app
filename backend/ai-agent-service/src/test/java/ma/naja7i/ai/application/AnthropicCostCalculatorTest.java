package ma.naja7i.ai.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Opus-tier pricing ($15 per million input tokens,
 * $75 per million output tokens) is applied with 6 decimals of precision.
 */
class AnthropicCostCalculatorTest {

    private final AnthropicCostCalculator calc = new AnthropicCostCalculator();

    @Test
    void oneMillionInputTokens_costsFifteenDollars() {
        BigDecimal cost = calc.compute(1_000_000, 0);
        assertThat(cost).isEqualByComparingTo(new BigDecimal("15.000000"));
    }

    @Test
    void oneMillionOutputTokens_costsSeventyFiveDollars() {
        BigDecimal cost = calc.compute(0, 1_000_000);
        assertThat(cost).isEqualByComparingTo(new BigDecimal("75.000000"));
    }

    @Test
    void mixedTokens_addCorrectly() {
        // 1000 in + 500 out => (1000 * 15 + 500 * 75) / 1_000_000
        //                   => (15_000 + 37_500) / 1_000_000
        //                   => 52_500 / 1_000_000 = 0.0525
        BigDecimal cost = calc.compute(1_000, 500);
        assertThat(cost).isEqualByComparingTo(new BigDecimal("0.052500"));
    }

    @Test
    void zeroTokens_returnZero() {
        assertThat(calc.compute(0, 0)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void negativeTokens_areClampedToZero() {
        assertThat(calc.compute(-100, -200)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}

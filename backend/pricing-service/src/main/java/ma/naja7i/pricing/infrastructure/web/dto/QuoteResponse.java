package ma.naja7i.pricing.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Detailed quote returned by {@code /pricing/quote}. All amounts are doubled:
 * raw centimes (machine) + human-readable MAD string with 2 decimals.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuoteResponse(
        long baseAmountMinor,
        String baseAmountMad,
        long platformFeeMinor,
        String platformFeeMad,
        long totalAmountMinor,
        String totalAmountMad,
        String currency,
        Breakdown breakdown
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Breakdown(
            String source,            // "PACK" | "RATE" | "RATE_FALLBACK_DAY_NULL"
            UUID rateId,
            UUID packId,
            Integer sessionsCount,
            BigDecimal platformFeePct,
            long platformFeeFixedMinor
    ) {
    }
}

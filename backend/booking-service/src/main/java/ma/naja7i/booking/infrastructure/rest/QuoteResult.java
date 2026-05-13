package ma.naja7i.booking.infrastructure.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Typed projection of {@code pricing-service}'s {@code QuoteResponse}. We keep only
 * the fields booking-service needs (base/fee/total in centimes + currency).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuoteResult(
        long baseAmountMinor,
        long platformFeeMinor,
        long totalAmountMinor,
        String currency
) {
}

package ma.naja7i.pricing.infrastructure.web.mapper;

import ma.naja7i.pricing.domain.MarketSuggestion;
import ma.naja7i.pricing.domain.PricingPack;
import ma.naja7i.pricing.domain.ProfPricing;
import ma.naja7i.pricing.infrastructure.web.dto.MarketSuggestionView;
import ma.naja7i.pricing.infrastructure.web.dto.PackView;
import ma.naja7i.pricing.infrastructure.web.dto.RateView;

/**
 * Hand-coded mappers — the BigDecimal/long conversions are awkward in MapStruct
 * and we only have three views, so plain Java keeps the wiring explicit and the
 * money rounding rule auditable.
 */
public final class PricingMappers {

    private PricingMappers() {}

    public static RateView toRateView(ProfPricing p) {
        if (p == null) return null;
        long minor = p.getAmountMinor() == null ? 0L : p.getAmountMinor();
        return new RateView(
                p.getId(),
                p.getProfId(),
                p.getSubjectCode(),
                p.getLevel(),
                p.getModality(),
                p.getDurationMinutes() == null ? null : p.getDurationMinutes().intValue(),
                p.getDayOfWeek() == null ? null : p.getDayOfWeek().intValue(),
                minor,
                MoneyConverter.minor2Mad(minor),
                p.getCurrency(),
                p.isActive(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    public static PackView toPackView(PricingPack p) {
        if (p == null) return null;
        long minor = p.getTotalAmountMinor() == null ? 0L : p.getTotalAmountMinor();
        return new PackView(
                p.getId(),
                p.getProfId(),
                p.getSubjectCode(),
                p.getLevel(),
                p.getModality(),
                p.getSessionsCount() == null ? null : p.getSessionsCount().intValue(),
                p.getDiscountPct(),
                minor,
                MoneyConverter.minor2Mad(minor),
                p.getCurrency(),
                p.isActive(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    public static MarketSuggestionView toMarketSuggestionView(MarketSuggestion s) {
        if (s == null) return null;
        long minor = s.getSuggestedAmountMinor() == null ? 0L : s.getSuggestedAmountMinor();
        return new MarketSuggestionView(
                s.getId(),
                s.getProfId(),
                s.getSubjectCode(),
                s.getLevel(),
                s.getModality(),
                s.getDurationMinutes() == null ? null : s.getDurationMinutes().intValue(),
                minor,
                MoneyConverter.minor2Mad(minor),
                s.getComputedAt(),
                s.getSource()
        );
    }
}

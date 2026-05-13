package ma.naja7i.pricing.infrastructure.web.dto;

import java.util.List;

/** Convenience aggregate returned by {@code GET /api/v1/pricing/me}. */
public record MyPricingView(List<RateView> rates, List<PackView> packs) {
}

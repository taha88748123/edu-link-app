package ma.naja7i.pricing.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.pricing.config.PricingProperties;
import ma.naja7i.pricing.domain.MarketSuggestion;
import ma.naja7i.pricing.domain.PlatformFee;
import ma.naja7i.pricing.domain.PricingPack;
import ma.naja7i.pricing.domain.ProfPricing;
import ma.naja7i.pricing.infrastructure.persistence.MarketSuggestionRepository;
import ma.naja7i.pricing.infrastructure.persistence.PlatformFeeRepository;
import ma.naja7i.pricing.infrastructure.persistence.PricingPackRepository;
import ma.naja7i.pricing.infrastructure.persistence.ProfPricingRepository;
import ma.naja7i.pricing.infrastructure.web.dto.MarketSuggestionView;
import ma.naja7i.pricing.infrastructure.web.dto.QuoteRequest;
import ma.naja7i.pricing.infrastructure.web.dto.QuoteResponse;
import ma.naja7i.pricing.infrastructure.web.mapper.MoneyConverter;
import ma.naja7i.pricing.infrastructure.web.mapper.PricingMappers;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

/**
 * Price-quote engine.
 *
 * <p>Selection order:
 * <ol>
 *   <li>If {@code packId} is supplied, the pack overrides any individual rate. The unit price
 *       used is {@code total_amount_minor / sessions_count} with HALF_UP rounding.</li>
 *   <li>Otherwise we look up {@code prof_pricing} with an exact (prof, subject, level,
 *       modality, duration, dayOfWeek) match; if missing, we fall back to the row with
 *       {@code day_of_week IS NULL}.</li>
 *   <li>If neither matches, we throw a normalised business error so booking-service can
 *       surface a useful message.</li>
 * </ol>
 *
 * <p>Platform fee: read from {@code platform_fees WHERE name='STANDARD' AND active=true}.
 * If no row exists, we fall back to the configured percentage (default 10 %).</p>
 *
 * <p>All math is in long centimes; we only touch BigDecimal to apply the percentage.</p>
 */
@Service
@RequiredArgsConstructor
public class QuoteService {

    /** Sentinel error code used by booking-service consumers to detect "no rate". */
    public static final String PRICING_RATE_NOT_FOUND = "PRICING_RATE_NOT_FOUND";

    private static final String STANDARD_FEE_NAME = "STANDARD";

    private final ProfPricingRepository rateRepo;
    private final PricingPackRepository packRepo;
    private final PlatformFeeRepository feeRepo;
    private final MarketSuggestionRepository suggestionRepo;
    private final PricingProperties props;

    // -------------------------------------------------------------------------
    // Quote
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public QuoteResponse quote(QuoteRequest req) {
        BaseAmount base = resolveBase(req);
        FeeContext fee = resolveFee();

        long feeMinor = computeFee(base.amountMinor, fee);
        long totalMinor = base.amountMinor + feeMinor;

        QuoteResponse.Breakdown breakdown = new QuoteResponse.Breakdown(
                base.source,
                base.rateId,
                base.packId,
                base.sessionsCount,
                fee.percent,
                fee.fixedMinor
        );

        return new QuoteResponse(
                base.amountMinor,
                MoneyConverter.minor2Mad(base.amountMinor),
                feeMinor,
                MoneyConverter.minor2Mad(feeMinor),
                totalMinor,
                MoneyConverter.minor2Mad(totalMinor),
                "MAD",
                breakdown);
    }

    // -------------------------------------------------------------------------
    // Market suggestion
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public MarketSuggestionView marketSuggestion(UUID profId,
                                                 String subjectCode,
                                                 SchoolLevel level,
                                                 Modality modality,
                                                 Integer durationMinutes) {
        MarketSuggestion ms = suggestionRepo
                .findFirstByProfIdAndSubjectCodeAndLevelAndModalityAndDurationMinutesOrderByComputedAtDesc(
                        profId, subjectCode, level, modality,
                        durationMinutes == null ? null : durationMinutes.shortValue())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "No market suggestion available",
                        Map.of(
                                "profId", profId.toString(),
                                "subjectCode", subjectCode,
                                "level", level.name(),
                                "modality", modality.name(),
                                "durationMinutes", durationMinutes
                        )));
        return PricingMappers.toMarketSuggestionView(ms);
    }

    // =========================================================================
    // Internals
    // =========================================================================
    private BaseAmount resolveBase(QuoteRequest req) {
        if (req.packId() != null) {
            PricingPack pack = packRepo.findById(req.packId())
                    .filter(PricingPack::isActive)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Pack not found or inactive",
                            Map.of("packId", req.packId().toString())));

            // Validate consistency: pack must belong to the prof and match the booking shape.
            if (!pack.getProfId().equals(req.profId())
                    || !pack.getSubjectCode().equals(req.subjectCode())
                    || pack.getLevel() != req.level()
                    || pack.getModality() != req.modality()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Pack does not match booking shape (prof, subject, level, modality)",
                        Map.of("packId", req.packId().toString()));
            }

            short sessions = pack.getSessionsCount() == null ? 1 : pack.getSessionsCount();
            if (sessions <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Pack has invalid sessions_count",
                        Map.of("packId", pack.getId().toString()));
            }
            long unitMinor = BigDecimal.valueOf(pack.getTotalAmountMinor())
                    .divide(BigDecimal.valueOf(sessions), 0, RoundingMode.HALF_UP)
                    .longValueExact();
            return new BaseAmount(unitMinor, "PACK", null, pack.getId(), (int) sessions);
        }

        Short dow = req.dayOfWeek() == null ? null : req.dayOfWeek().shortValue();
        Short dur = req.durationMinutes().shortValue();

        if (dow != null) {
            var exact = rateRepo
                    .findFirstByProfIdAndSubjectCodeAndLevelAndModalityAndDurationMinutesAndDayOfWeekAndActiveTrue(
                            req.profId(), req.subjectCode(), req.level(), req.modality(), dur, dow);
            if (exact.isPresent()) {
                ProfPricing p = exact.get();
                return new BaseAmount(p.getAmountMinor(), "RATE", p.getId(), null, null);
            }
        }
        ProfPricing fallback = rateRepo
                .findFirstByProfIdAndSubjectCodeAndLevelAndModalityAndDurationMinutesAndDayOfWeekIsNullAndActiveTrue(
                        req.profId(), req.subjectCode(), req.level(), req.modality(), dur)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        PRICING_RATE_NOT_FOUND,
                        Map.of(
                                "profId", req.profId().toString(),
                                "subjectCode", req.subjectCode(),
                                "level", req.level().name(),
                                "modality", req.modality().name(),
                                "durationMinutes", req.durationMinutes(),
                                "dayOfWeek", String.valueOf(req.dayOfWeek())
                        )));
        return new BaseAmount(fallback.getAmountMinor(),
                dow == null ? "RATE" : "RATE_FALLBACK_DAY_NULL",
                fallback.getId(), null, null);
    }

    private FeeContext resolveFee() {
        return feeRepo.findFirstByNameAndActiveTrue(STANDARD_FEE_NAME)
                .map(this::toCtx)
                .orElseGet(() -> new FeeContext(props.getDefaultPlatformFeePct(), 0L));
    }

    private FeeContext toCtx(PlatformFee fee) {
        BigDecimal pct = fee.getPercent() == null ? BigDecimal.ZERO : fee.getPercent();
        long fixed = fee.getFixedMinor() == null ? 0L : fee.getFixedMinor();
        return new FeeContext(pct, fixed);
    }

    /**
     * Fee = round(base * pct / 100, HALF_UP) + fixed.
     * Computation happens in BigDecimal to avoid long overflow on huge amounts,
     * then results are bounded back to long centimes.
     */
    private long computeFee(long baseMinor, FeeContext fee) {
        BigDecimal pctPart = BigDecimal.valueOf(baseMinor)
                .multiply(fee.percent)
                .divide(BigDecimal.valueOf(100L), 0, RoundingMode.HALF_UP);
        return pctPart.longValueExact() + fee.fixedMinor;
    }

    private record BaseAmount(long amountMinor, String source, UUID rateId, UUID packId, Integer sessionsCount) {}
    private record FeeContext(BigDecimal percent, long fixedMinor) {}
}

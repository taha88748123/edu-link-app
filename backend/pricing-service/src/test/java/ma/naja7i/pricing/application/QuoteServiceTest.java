package ma.naja7i.pricing.application;

import ma.naja7i.pricing.config.PricingProperties;
import ma.naja7i.pricing.domain.PlatformFee;
import ma.naja7i.pricing.domain.PricingPack;
import ma.naja7i.pricing.domain.ProfPricing;
import ma.naja7i.pricing.infrastructure.persistence.MarketSuggestionRepository;
import ma.naja7i.pricing.infrastructure.persistence.PlatformFeeRepository;
import ma.naja7i.pricing.infrastructure.persistence.PricingPackRepository;
import ma.naja7i.pricing.infrastructure.persistence.ProfPricingRepository;
import ma.naja7i.pricing.infrastructure.web.dto.QuoteRequest;
import ma.naja7i.pricing.infrastructure.web.dto.QuoteResponse;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuoteServiceTest {

    private ProfPricingRepository rateRepo;
    private PricingPackRepository packRepo;
    private PlatformFeeRepository feeRepo;
    private MarketSuggestionRepository suggestionRepo;
    private PricingProperties props;
    private QuoteService service;

    private UUID profId;

    @BeforeEach
    void setUp() {
        rateRepo = mock(ProfPricingRepository.class);
        packRepo = mock(PricingPackRepository.class);
        feeRepo = mock(PlatformFeeRepository.class);
        suggestionRepo = mock(MarketSuggestionRepository.class);
        props = new PricingProperties();
        props.setDefaultPlatformFeePct(new BigDecimal("10.0"));
        service = new QuoteService(rateRepo, packRepo, feeRepo, suggestionRepo, props);

        profId = UUID.randomUUID();
    }

    // ------------------------------------------------------------------------
    // Happy path: exact rate + standard 10% fee from DB
    // ------------------------------------------------------------------------
    @Test
    void happyPath_exactRate_with_standardFee_from_db() {
        ProfPricing rate = newRate(15000L, (short) 60, (short) 1); // 150.00 MAD, Tuesday
        when(rateRepo.findFirstByProfIdAndSubjectCodeAndLevelAndModalityAndDurationMinutesAndDayOfWeekAndActiveTrue(
                eq(profId), eq("MATH"), eq(SchoolLevel.LYCEE), eq(Modality.ONLINE),
                eq((short) 60), eq((short) 1))).thenReturn(Optional.of(rate));
        when(feeRepo.findFirstByNameAndActiveTrue("STANDARD"))
                .thenReturn(Optional.of(newFee("10.0", 0L)));

        QuoteResponse out = service.quote(new QuoteRequest(
                profId, "MATH", SchoolLevel.LYCEE, Modality.ONLINE, 60, 1, null));

        assertThat(out.baseAmountMinor()).isEqualTo(15000L);
        assertThat(out.baseAmountMad()).isEqualTo("150.00");
        assertThat(out.platformFeeMinor()).isEqualTo(1500L); // 10%
        assertThat(out.platformFeeMad()).isEqualTo("15.00");
        assertThat(out.totalAmountMinor()).isEqualTo(16500L);
        assertThat(out.totalAmountMad()).isEqualTo("165.00");
        assertThat(out.currency()).isEqualTo("MAD");
        assertThat(out.breakdown().source()).isEqualTo("RATE");
        assertThat(out.breakdown().rateId()).isEqualTo(rate.getId());
        assertThat(out.breakdown().platformFeePct()).isEqualByComparingTo("10.0");
    }

    // ------------------------------------------------------------------------
    // packId overrides rate
    // ------------------------------------------------------------------------
    @Test
    void packOverridesRate_unitPrice_isTotalDividedBySessions() {
        PricingPack pack = newPack(50000L, (short) 5); // 500 MAD / 5 = 100 MAD per unit
        when(packRepo.findById(pack.getId())).thenReturn(Optional.of(pack));
        when(feeRepo.findFirstByNameAndActiveTrue("STANDARD"))
                .thenReturn(Optional.of(newFee("10.0", 0L)));

        QuoteResponse out = service.quote(new QuoteRequest(
                profId, "MATH", SchoolLevel.LYCEE, Modality.ONLINE, 60, null, pack.getId()));

        assertThat(out.baseAmountMinor()).isEqualTo(10000L);
        assertThat(out.platformFeeMinor()).isEqualTo(1000L);
        assertThat(out.totalAmountMinor()).isEqualTo(11000L);
        assertThat(out.breakdown().source()).isEqualTo("PACK");
        assertThat(out.breakdown().packId()).isEqualTo(pack.getId());
        assertThat(out.breakdown().sessionsCount()).isEqualTo(5);
    }

    // ------------------------------------------------------------------------
    // dayOfWeek given but exact row missing -> fallback to day_of_week IS NULL
    // ------------------------------------------------------------------------
    @Test
    void dayOfWeekFallback_to_dayNull_row() {
        // exact day match returns empty
        when(rateRepo.findFirstByProfIdAndSubjectCodeAndLevelAndModalityAndDurationMinutesAndDayOfWeekAndActiveTrue(
                eq(profId), eq("MATH"), eq(SchoolLevel.LYCEE), eq(Modality.ONLINE),
                eq((short) 60), eq((short) 3))).thenReturn(Optional.empty());
        // fallback returns a row
        ProfPricing fallback = newRate(20000L, (short) 60, null);
        when(rateRepo.findFirstByProfIdAndSubjectCodeAndLevelAndModalityAndDurationMinutesAndDayOfWeekIsNullAndActiveTrue(
                eq(profId), eq("MATH"), eq(SchoolLevel.LYCEE), eq(Modality.ONLINE), eq((short) 60)))
                .thenReturn(Optional.of(fallback));
        when(feeRepo.findFirstByNameAndActiveTrue("STANDARD")).thenReturn(Optional.empty());

        QuoteResponse out = service.quote(new QuoteRequest(
                profId, "MATH", SchoolLevel.LYCEE, Modality.ONLINE, 60, 3, null));

        assertThat(out.baseAmountMinor()).isEqualTo(20000L);
        assertThat(out.platformFeeMinor()).isEqualTo(2000L);   // 10% default (no row)
        assertThat(out.breakdown().source()).isEqualTo("RATE_FALLBACK_DAY_NULL");
        assertThat(out.breakdown().rateId()).isEqualTo(fallback.getId());
    }

    // ------------------------------------------------------------------------
    // Missing rate -> throws PRICING_RATE_NOT_FOUND (RESOURCE_NOT_FOUND)
    // ------------------------------------------------------------------------
    @Test
    void missingRate_throwsResourceNotFound_with_sentinelMessage() {
        when(rateRepo.findFirstByProfIdAndSubjectCodeAndLevelAndModalityAndDurationMinutesAndDayOfWeekIsNullAndActiveTrue(
                any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        QuoteRequest req = new QuoteRequest(
                profId, "MATH", SchoolLevel.LYCEE, Modality.ONLINE, 60, null, null);

        assertThatThrownBy(() -> service.quote(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(QuoteService.PRICING_RATE_NOT_FOUND)
                .extracting(ex -> ((BusinessException) ex).code()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    // ------------------------------------------------------------------------
    // Rounding: base=150.55 MAD = 15055 centimes, 10% = 1505.5 -> HALF_UP -> 1506
    // ------------------------------------------------------------------------
    @Test
    void rounding_isHalfUp_onPercent() {
        ProfPricing rate = newRate(15055L, (short) 60, null);
        when(rateRepo.findFirstByProfIdAndSubjectCodeAndLevelAndModalityAndDurationMinutesAndDayOfWeekIsNullAndActiveTrue(
                eq(profId), eq("MATH"), eq(SchoolLevel.LYCEE), eq(Modality.ONLINE), eq((short) 60)))
                .thenReturn(Optional.of(rate));
        when(feeRepo.findFirstByNameAndActiveTrue("STANDARD"))
                .thenReturn(Optional.of(newFee("10.00", 0L)));

        QuoteResponse out = service.quote(new QuoteRequest(
                profId, "MATH", SchoolLevel.LYCEE, Modality.ONLINE, 60, null, null));

        assertThat(out.platformFeeMinor()).isEqualTo(1506L);
        assertThat(out.totalAmountMinor()).isEqualTo(15055L + 1506L);
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    private ProfPricing newRate(long amountMinor, Short duration, Short dayOfWeek) {
        ProfPricing p = ProfPricing.builder()
                .id(UUID.randomUUID())
                .profId(profId)
                .subjectCode("MATH")
                .level(SchoolLevel.LYCEE)
                .modality(Modality.ONLINE)
                .durationMinutes(duration)
                .dayOfWeek(dayOfWeek)
                .amountMinor(amountMinor)
                .currency("MAD")
                .active(true)
                .version(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return p;
    }

    private PricingPack newPack(long totalMinor, short sessions) {
        return PricingPack.builder()
                .id(UUID.randomUUID())
                .profId(profId)
                .subjectCode("MATH")
                .level(SchoolLevel.LYCEE)
                .modality(Modality.ONLINE)
                .sessionsCount(sessions)
                .discountPct(BigDecimal.ZERO)
                .totalAmountMinor(totalMinor)
                .currency("MAD")
                .active(true)
                .version(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private PlatformFee newFee(String pct, long fixedMinor) {
        return PlatformFee.builder()
                .id(UUID.randomUUID())
                .name("STANDARD")
                .percent(new BigDecimal(pct))
                .fixedMinor(fixedMinor)
                .active(true)
                .validFrom(Instant.now())
                .createdAt(Instant.now())
                .build();
    }
}

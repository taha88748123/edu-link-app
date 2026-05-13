package ma.naja7i.pricing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.naja7i.pricing.application.PackService;
import ma.naja7i.pricing.application.PricingService;
import ma.naja7i.pricing.infrastructure.web.dto.PackView;
import ma.naja7i.pricing.infrastructure.web.dto.RateView;
import ma.naja7i.pricing.infrastructure.web.mapper.PricingMappers;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Public, authenticated-user endpoints to browse a prof's rates and packs.
 * Mounted under {@code /api/v1/pricing/profs}.
 */
@RestController
@RequestMapping("/api/v1/pricing/profs")
@RequiredArgsConstructor
@Tag(name = "Pricing — Public catalog", description = "Read-only view of a prof's rates and packs.")
public class ProfPricingPublicController {

    private final PricingService pricingService;
    private final PackService packService;

    @GetMapping("/{profId}/rates")
    @Operation(summary = "List active rates of a prof (filterable)")
    public ApiResponse<List<RateView>> rates(
            @PathVariable UUID profId,
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) SchoolLevel level,
            @RequestParam(required = false) Modality modality) {
        List<RateView> out = pricingService.searchActive(profId, subjectCode, level, modality)
                .stream().map(PricingMappers::toRateView).toList();
        return ApiResponse.of(out);
    }

    @GetMapping("/{profId}/packs")
    @Operation(summary = "List active session packs of a prof (filterable)")
    public ApiResponse<List<PackView>> packs(
            @PathVariable UUID profId,
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) SchoolLevel level,
            @RequestParam(required = false) Modality modality) {
        List<PackView> out = packService.searchActive(profId, subjectCode, level, modality)
                .stream().map(PricingMappers::toPackView).toList();
        return ApiResponse.of(out);
    }
}

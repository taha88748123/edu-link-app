package ma.naja7i.pricing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.pricing.application.PackService;
import ma.naja7i.pricing.application.PricingService;
import ma.naja7i.pricing.infrastructure.web.dto.CreateRateRequest;
import ma.naja7i.pricing.infrastructure.web.dto.MyPricingView;
import ma.naja7i.pricing.infrastructure.web.dto.PackView;
import ma.naja7i.pricing.infrastructure.web.dto.RateView;
import ma.naja7i.pricing.infrastructure.web.dto.UpdateRateRequest;
import ma.naja7i.pricing.infrastructure.web.mapper.PricingMappers;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.security.UserContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Authenticated PROF endpoints for managing my own rates.
 * Mounted under {@code /api/v1/pricing}.
 */
@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
@Tag(name = "Pricing — Prof rates", description = "Prof-owned single-session rate rows.")
public class PricingController {

    private final PricingService pricingService;
    private final PackService packService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "List MY active rates and packs (PROF)")
    public ApiResponse<MyPricingView> me(@AuthUser UserContext ctx) {
        requireProf(ctx);
        List<RateView> rates = pricingService.listActiveByProf(ctx.userId()).stream()
                .map(PricingMappers::toRateView).toList();
        List<PackView> packs = packService.listActiveByProf(ctx.userId()).stream()
                .map(PricingMappers::toPackView).toList();
        return ApiResponse.of(new MyPricingView(rates, packs));
    }

    @PostMapping("/rates")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Create a single-session rate (PROF)")
    public ResponseEntity<ApiResponse<RateView>> createRate(
            @AuthUser UserContext ctx,
            @Valid @RequestBody CreateRateRequest body) {
        requireProf(ctx);
        RateView view = PricingMappers.toRateView(pricingService.create(ctx.userId(), body));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(view));
    }

    @PutMapping("/rates/{id}")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Update a rate I own (PROF)")
    public ApiResponse<RateView> updateRate(
            @AuthUser UserContext ctx,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRateRequest body) {
        requireProf(ctx);
        return ApiResponse.of(PricingMappers.toRateView(pricingService.update(ctx.userId(), id, body)));
    }

    @DeleteMapping("/rates/{id}")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Soft-delete (deactivate) a rate I own (PROF)")
    public ResponseEntity<Void> deleteRate(
            @AuthUser UserContext ctx,
            @PathVariable UUID id) {
        requireProf(ctx);
        pricingService.softDelete(ctx.userId(), id);
        return ResponseEntity.noContent().build();
    }

    /** Belt-and-braces: even if a non-PROF token slips past @PreAuthorize we reject here. */
    private static void requireProf(UserContext ctx) {
        if (ctx == null || ctx.role() != Role.PROF) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "PROF role required");
        }
    }
}

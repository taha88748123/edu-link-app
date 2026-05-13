package ma.naja7i.pricing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.pricing.application.PackService;
import ma.naja7i.pricing.infrastructure.web.dto.CreatePackRequest;
import ma.naja7i.pricing.infrastructure.web.dto.PackView;
import ma.naja7i.pricing.infrastructure.web.dto.UpdatePackRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Authenticated PROF endpoints for managing session packs.
 * Mounted under {@code /api/v1/pricing/packs}.
 */
@RestController
@RequestMapping("/api/v1/pricing/packs")
@RequiredArgsConstructor
@Tag(name = "Pricing — Packs", description = "Prof-owned multi-session bundles.")
public class PackController {

    private final PackService packService;

    @PostMapping
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Create a session pack (PROF)")
    public ResponseEntity<ApiResponse<PackView>> create(
            @AuthUser UserContext ctx,
            @Valid @RequestBody CreatePackRequest body) {
        requireProf(ctx);
        PackView view = PricingMappers.toPackView(packService.create(ctx.userId(), body));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(view));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Update a pack I own (PROF)")
    public ApiResponse<PackView> update(
            @AuthUser UserContext ctx,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePackRequest body) {
        requireProf(ctx);
        return ApiResponse.of(PricingMappers.toPackView(packService.update(ctx.userId(), id, body)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Soft-delete a pack I own (PROF)")
    public ResponseEntity<Void> delete(
            @AuthUser UserContext ctx,
            @PathVariable UUID id) {
        requireProf(ctx);
        packService.softDelete(ctx.userId(), id);
        return ResponseEntity.noContent().build();
    }

    private static void requireProf(UserContext ctx) {
        if (ctx == null || ctx.role() != Role.PROF) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE, "PROF role required");
        }
    }
}

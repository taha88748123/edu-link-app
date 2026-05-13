package ma.naja7i.calendar.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.calendar.application.AvailabilityRuleService;
import ma.naja7i.calendar.application.SlotMaterializer;
import ma.naja7i.calendar.infrastructure.web.dto.AvailabilityRuleView;
import ma.naja7i.calendar.infrastructure.web.dto.CreateAvailabilityRuleRequest;
import ma.naja7i.calendar.infrastructure.web.dto.GenerateSlotsResponse;
import ma.naja7i.calendar.infrastructure.web.dto.UpdateAvailabilityRuleRequest;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
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

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
@Tag(name = "Availability rules (PROF)",
        description = "PROF-owned recurrence rules that drive slot materialization")
public class AvailabilityRuleController {

    private final AvailabilityRuleService ruleService;
    private final SlotMaterializer materializer;

    @PostMapping("/availability-rules")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Create a WEEKLY or ONE_OFF rule")
    public ApiResponse<AvailabilityRuleView> create(@AuthUser UserContext ctx,
                                                    @Valid @RequestBody CreateAvailabilityRuleRequest req) {
        return ApiResponse.of(AvailabilityRuleView.of(ruleService.create(ctx.userId(), req)));
    }

    @GetMapping("/availability-rules/me")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "List caller's availability rules")
    public ApiResponse<List<AvailabilityRuleView>> listMine(@AuthUser UserContext ctx) {
        List<AvailabilityRuleView> views = ruleService.listOwn(ctx.userId()).stream()
                .map(AvailabilityRuleView::of)
                .toList();
        return ApiResponse.of(views);
    }

    @PutMapping("/availability-rules/{id}")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Update an availability rule")
    public ApiResponse<AvailabilityRuleView> update(@AuthUser UserContext ctx,
                                                    @PathVariable UUID id,
                                                    @Valid @RequestBody UpdateAvailabilityRuleRequest req) {
        return ApiResponse.of(AvailabilityRuleView.of(ruleService.update(ctx.userId(), id, req)));
    }

    @DeleteMapping("/availability-rules/{id}")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Soft delete a rule (sets active=false, blocks future generated slots)")
    public ApiResponse<Void> delete(@AuthUser UserContext ctx, @PathVariable UUID id) {
        ruleService.softDelete(ctx.userId(), id);
        return ApiResponse.of(null);
    }

    @PostMapping("/slots:generate")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Materialize slots over the next N days from active rules (idempotent)")
    public ApiResponse<GenerateSlotsResponse> generateSlots(@AuthUser UserContext ctx) {
        SlotMaterializer.Stats stats = materializer.generateFor(ctx.userId());
        return ApiResponse.of(new GenerateSlotsResponse(
                ctx.userId(), stats.created(), stats.skipped()));
    }
}

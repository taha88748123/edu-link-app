package ma.naja7i.calendar.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.naja7i.calendar.application.SlotService;
import ma.naja7i.calendar.domain.Slot;
import ma.naja7i.calendar.domain.SlotStatus;
import ma.naja7i.calendar.infrastructure.web.dto.SlotView;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
@Tag(name = "Slots", description = "Slot lifecycle endpoints")
public class SlotController {

    private final SlotService slotService;

    /** Public catalog: only AVAILABLE slots. */
    @GetMapping("/profs/{profId}/slots")
    @Operation(summary = "List AVAILABLE slots of a prof in a time window")
    public ApiResponse<List<SlotView>> publicSlots(@PathVariable UUID profId,
                                                   @RequestParam Instant from,
                                                   @RequestParam Instant to,
                                                   @RequestParam(required = false) Modality modality,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "50") int size) {
        Page<Slot> result = slotService.listAvailableForProf(profId, from, to, modality, page, size);
        return ApiResponse.of(
                result.map(SlotView::of).getContent(),
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/slots/me")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "List caller's slots (any status)")
    public ApiResponse<List<SlotView>> mySlots(@AuthUser UserContext ctx,
                                               @RequestParam Instant from,
                                               @RequestParam Instant to,
                                               @RequestParam(required = false) SlotStatus status,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "50") int size) {
        Page<Slot> result = slotService.listForOwner(ctx.userId(), from, to, status, page, size);
        return ApiResponse.of(
                result.map(SlotView::of).getContent(),
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @PostMapping("/slots/{id}:block")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Block an AVAILABLE slot")
    public ApiResponse<SlotView> block(@AuthUser UserContext ctx, @PathVariable UUID id) {
        return ApiResponse.of(SlotView.of(slotService.block(ctx.userId(), id)));
    }

    @PostMapping("/slots/{id}:unblock")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Unblock a BLOCKED slot")
    public ApiResponse<SlotView> unblock(@AuthUser UserContext ctx, @PathVariable UUID id) {
        return ApiResponse.of(SlotView.of(slotService.unblock(ctx.userId(), id)));
    }
}

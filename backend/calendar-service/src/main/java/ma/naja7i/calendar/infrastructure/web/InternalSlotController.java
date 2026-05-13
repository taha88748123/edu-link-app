package ma.naja7i.calendar.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.calendar.application.SlotService;
import ma.naja7i.calendar.infrastructure.web.dto.ConfirmSlotRequest;
import ma.naja7i.calendar.infrastructure.web.dto.ReleaseSlotRequest;
import ma.naja7i.calendar.infrastructure.web.dto.ReserveSlotRequest;
import ma.naja7i.calendar.infrastructure.web.dto.SlotView;
import ma.naja7i.shared.dto.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Service-to-service endpoints used by the booking-service.
 * Protected by {@link ma.naja7i.calendar.infrastructure.security.InternalKeyFilter}.
 */
@RestController
@RequestMapping("/api/v1/internal/calendar")
@RequiredArgsConstructor
@Tag(name = "Slot internal lifecycle",
        description = "S2S endpoints — booking-service drives reserve/confirm/release")
public class InternalSlotController {

    private final SlotService slotService;

    @PostMapping("/slots/{slotId}:reserve")
    @Operation(summary = "AVAILABLE → RESERVED_PENDING (sets reservedUntil = now + ttl)")
    public ApiResponse<SlotView> reserve(@PathVariable UUID slotId,
                                         @Valid @RequestBody ReserveSlotRequest req) {
        return ApiResponse.of(SlotView.of(slotService.reserve(slotId, req.bookingId())));
    }

    @PostMapping("/slots/{slotId}:confirm")
    @Operation(summary = "RESERVED_PENDING → BOOKED")
    public ApiResponse<SlotView> confirm(@PathVariable UUID slotId,
                                         @Valid @RequestBody ConfirmSlotRequest req) {
        return ApiResponse.of(SlotView.of(slotService.confirm(slotId, req.bookingId())));
    }

    @PostMapping("/slots/{slotId}:release")
    @Operation(summary = "RESERVED_PENDING|BOOKED → AVAILABLE (emits slot.released.v1)")
    public ApiResponse<SlotView> release(@PathVariable UUID slotId,
                                         @RequestBody(required = false) ReleaseSlotRequest req) {
        String reason = req == null ? null : req.reason();
        return ApiResponse.of(SlotView.of(slotService.release(slotId, reason)));
    }
}

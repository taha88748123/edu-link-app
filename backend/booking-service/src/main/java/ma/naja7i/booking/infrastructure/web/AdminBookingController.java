package ma.naja7i.booking.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.booking.application.BookingService;
import ma.naja7i.booking.application.BookingViewMasker;
import ma.naja7i.booking.application.DisputeService;
import ma.naja7i.booking.domain.Booking;
import ma.naja7i.booking.infrastructure.web.dto.BookingView;
import ma.naja7i.booking.infrastructure.web.dto.DisputeView;
import ma.naja7i.booking.infrastructure.web.dto.ResolveDisputeRequest;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.dto.BookingStatus;
import ma.naja7i.shared.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ADMIN endpoints: dispute resolution + filtered booking list.
 */
@RestController
@RequestMapping("/api/v1/bookings/admin")
@RequiredArgsConstructor
@Tag(name = "Bookings — Admin", description = "Admin queries + dispute resolution")
public class AdminBookingController {

    private final BookingService bookingService;
    private final DisputeService disputeService;
    private final BookingViewMasker masker;

    @PostMapping("/{id}/disputes/{disputeId}:resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolve a dispute (REFUND / PAY_PROF / SPLIT)")
    public ApiResponse<DisputeView> resolveDispute(@AuthUser UserContext ctx,
                                                   @PathVariable("id") UUID bookingId,
                                                   @PathVariable("disputeId") UUID disputeId,
                                                   @Valid @RequestBody ResolveDisputeRequest body) {
        var d = disputeService.resolve(bookingId, disputeId, ctx.userId(), body);
        return ApiResponse.of(masker.toDisputeView(d));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin booking list (status / from / to filters)")
    public ApiResponse<List<BookingView>> listForAdmin(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<Booking> result = bookingService.listForAdmin(status, from, to, page, size);
        return ApiResponse.of(
                result.map(masker::toListView).getContent(),
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }
}

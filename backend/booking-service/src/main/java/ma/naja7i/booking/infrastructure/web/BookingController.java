package ma.naja7i.booking.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.booking.application.BookingService;
import ma.naja7i.booking.application.BookingViewMasker;
import ma.naja7i.booking.application.DisputeService;
import ma.naja7i.booking.domain.Booking;
import ma.naja7i.booking.infrastructure.web.dto.BookingDetailedView;
import ma.naja7i.booking.infrastructure.web.dto.BookingView;
import ma.naja7i.booking.infrastructure.web.dto.CancelRequest;
import ma.naja7i.booking.infrastructure.web.dto.CreateBookingRequest;
import ma.naja7i.booking.infrastructure.web.dto.DisputeRequest;
import ma.naja7i.booking.infrastructure.web.dto.DisputeView;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.dto.BookingStatus;
import ma.naja7i.shared.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * STUDENT-facing endpoints. Cancel and dispute also accept PROF callers.
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings — Student", description = "Create / list / cancel / confirm / dispute")
public class BookingController {

    private final BookingService bookingService;
    private final DisputeService disputeService;
    private final BookingViewMasker masker;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a booking (STUDENT)")
    public ApiResponse<BookingDetailedView> create(@AuthUser UserContext ctx,
                                                   @Valid @RequestBody CreateBookingRequest req) {
        Booking b = bookingService.create(ctx.userId(), req);
        return ApiResponse.of(masker.toDetailedView(b, ctx.role()));
    }

    @PostMapping("/{id}:cancel")
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @Operation(summary = "Cancel a booking (DEMANDEE / ACCEPTEE_PAR_PROF only)")
    public ApiResponse<BookingDetailedView> cancel(@AuthUser UserContext ctx,
                                                   @PathVariable("id") UUID bookingId,
                                                   @RequestBody(required = false) CancelRequest body) {
        String reason = body == null ? null : body.reason();
        Booking b = bookingService.cancel(bookingId, ctx.userId(), ctx.role(), reason);
        return ApiResponse.of(masker.toDetailedView(b, ctx.role()));
    }

    @PostMapping("/{id}:confirm")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Confirm a completed booking (TERMINEE -> CONFIRMEE)")
    public ApiResponse<BookingDetailedView> confirm(@AuthUser UserContext ctx,
                                                    @PathVariable("id") UUID bookingId) {
        Booking b = bookingService.confirm(bookingId, ctx.userId());
        return ApiResponse.of(masker.toDetailedView(b, ctx.role()));
    }

    @PostMapping("/{id}:dispute")
    @PreAuthorize("hasAnyRole('STUDENT','PROF')")
    @Operation(summary = "Open a dispute on a booking")
    public ApiResponse<DisputeView> dispute(@AuthUser UserContext ctx,
                                            @PathVariable("id") UUID bookingId,
                                            @Valid @RequestBody DisputeRequest body) {
        var d = disputeService.openDispute(bookingId, ctx.userId(), ctx.role(),
                body.cause(), body.evidenceUrls());
        return ApiResponse.of(masker.toDisputeView(d));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT','PROF')")
    @Operation(summary = "List my bookings (paginated)")
    public ApiResponse<List<BookingView>> myBookings(@AuthUser UserContext ctx,
                                                     @RequestParam(required = false) BookingStatus status,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        Page<Booking> result = bookingService.listForCaller(ctx.userId(), ctx.role(), status, page, size);
        return ApiResponse.of(
                result.map(masker::toListView).getContent(),
                new ApiResponse.PageMeta(result.getNumber(), result.getSize(), result.getTotalElements()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','PROF','ADMIN')")
    @Operation(summary = "Booking detail (must belong to me or be admin)")
    public ApiResponse<BookingDetailedView> get(@AuthUser UserContext ctx,
                                                @PathVariable("id") UUID bookingId) {
        Booking b = bookingService.getOwned(bookingId, ctx.userId(), ctx.role());
        return ApiResponse.of(masker.toDetailedView(b, ctx.role()));
    }
}

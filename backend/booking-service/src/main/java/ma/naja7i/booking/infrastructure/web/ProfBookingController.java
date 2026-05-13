package ma.naja7i.booking.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ma.naja7i.booking.application.BookingService;
import ma.naja7i.booking.application.BookingViewMasker;
import ma.naja7i.booking.domain.Booking;
import ma.naja7i.booking.infrastructure.web.dto.BookingDetailedView;
import ma.naja7i.booking.infrastructure.web.dto.DeclineRequest;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * PROF-facing endpoints: accept / decline / start a booking.
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings — Prof", description = "Accept / decline / start")
public class ProfBookingController {

    private final BookingService bookingService;
    private final BookingViewMasker masker;

    @PostMapping("/{id}:accept")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Accept a DEMANDEE booking (PROF)")
    public ApiResponse<BookingDetailedView> accept(@AuthUser UserContext ctx,
                                                   @PathVariable("id") UUID bookingId) {
        Booking b = bookingService.accept(bookingId, ctx.userId());
        return ApiResponse.of(masker.toDetailedView(b, ctx.role()));
    }

    @PostMapping("/{id}:decline")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Decline a DEMANDEE booking (PROF)")
    public ApiResponse<BookingDetailedView> decline(@AuthUser UserContext ctx,
                                                    @PathVariable("id") UUID bookingId,
                                                    @RequestBody(required = false) DeclineRequest body) {
        String reason = body == null ? null : body.reason();
        Booking b = bookingService.decline(bookingId, ctx.userId(), reason);
        return ApiResponse.of(masker.toDetailedView(b, ctx.role()));
    }

    @PostMapping("/{id}:start")
    @PreAuthorize("hasRole('PROF')")
    @Operation(summary = "Start an ACCEPTEE_PAR_PROF booking (PROF) — sets actual_start")
    public ApiResponse<BookingDetailedView> start(@AuthUser UserContext ctx,
                                                  @PathVariable("id") UUID bookingId) {
        Booking b = bookingService.start(bookingId, ctx.userId());
        return ApiResponse.of(masker.toDetailedView(b, ctx.role()));
    }
}

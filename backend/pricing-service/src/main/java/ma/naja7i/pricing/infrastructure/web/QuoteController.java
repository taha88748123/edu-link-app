package ma.naja7i.pricing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.pricing.application.QuoteService;
import ma.naja7i.pricing.infrastructure.web.dto.QuoteRequest;
import ma.naja7i.pricing.infrastructure.web.dto.QuoteResponse;
import ma.naja7i.shared.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public {@code POST /api/v1/pricing/quote} endpoint — any authenticated user can simulate a price.
 * Booking-service uses the internal counterpart in {@link InternalQuoteController}.
 */
@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
@Tag(name = "Pricing — Quote", description = "Simulate a booking price (rate or pack + platform fee).")
public class QuoteController {

    private final QuoteService quoteService;

    @PostMapping("/quote")
    @Operation(summary = "Compute a price quote for a booking shape")
    public ApiResponse<QuoteResponse> quote(@Valid @RequestBody QuoteRequest body) {
        return ApiResponse.of(quoteService.quote(body));
    }
}

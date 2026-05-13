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
 * Server-to-server quote endpoint used by booking-service to lock a booking price.
 * Guarded by {@code X-Internal-Key} via
 * {@link ma.naja7i.pricing.infrastructure.security.InternalKeyFilter}.
 */
@RestController
@RequestMapping("/api/v1/internal/pricing")
@RequiredArgsConstructor
@Tag(name = "Pricing — Internal", description = "Internal quote endpoint for backend services.")
public class InternalQuoteController {

    private final QuoteService quoteService;

    @PostMapping("/quote")
    @Operation(summary = "Compute a price quote (server-to-server, X-Internal-Key)")
    public ApiResponse<QuoteResponse> quote(@Valid @RequestBody QuoteRequest body) {
        return ApiResponse.of(quoteService.quote(body));
    }
}

package ma.naja7i.pricing.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import ma.naja7i.pricing.application.QuoteService;
import ma.naja7i.pricing.infrastructure.web.dto.MarketSuggestionView;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-only public endpoint exposing market suggestions computed by an external job.
 */
@RestController
@RequestMapping("/api/v1/pricing/profs")
@RequiredArgsConstructor
@Validated
@Tag(name = "Pricing — Market suggestion", description = "AI-computed price suggestion cache.")
public class MarketSuggestionController {

    private final QuoteService quoteService;

    @GetMapping("/{profId}/market-suggestion")
    @Operation(summary = "Latest market suggestion for (prof, subject, level, modality, duration)")
    public ApiResponse<MarketSuggestionView> marketSuggestion(
            @PathVariable UUID profId,
            @NotBlank @RequestParam String subjectCode,
            @NotNull @RequestParam SchoolLevel level,
            @NotNull @RequestParam Modality modality,
            @NotNull @Min(1) @Max(480) @RequestParam Integer durationMinutes) {
        return ApiResponse.of(quoteService.marketSuggestion(
                profId, subjectCode, level, modality, durationMinutes));
    }
}

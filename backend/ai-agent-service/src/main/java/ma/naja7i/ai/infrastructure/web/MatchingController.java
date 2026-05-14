package ma.naja7i.ai.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.ai.application.MatchingService;
import ma.naja7i.ai.infrastructure.web.dto.MatchingRequest;
import ma.naja7i.ai.infrastructure.web.dto.MatchingResponse;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Student-facing endpoint that returns a ranked list of profs based on
 * subject / level / modality plus optional soft filters (city, budget).
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "Matching", description = "Prof suggestion engine (MATCHING agent)")
public class MatchingController {

    private final MatchingService matchingService;

    @PostMapping("/matching/profs")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    @Operation(summary = "Suggest profs ranked by score, with cached responses for repeat filters")
    public ApiResponse<MatchingResponse> matchProfs(@AuthUser UserContext ctx,
                                                    @Valid @RequestBody MatchingRequest request) {
        MatchingResponse response = matchingService.match(ctx.userId(), request);
        return ApiResponse.of(response);
    }
}

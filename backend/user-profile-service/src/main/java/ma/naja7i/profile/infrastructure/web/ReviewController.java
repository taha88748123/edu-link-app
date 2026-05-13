package ma.naja7i.profile.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.naja7i.profile.application.ProfReviewService;
import ma.naja7i.profile.domain.Review;
import ma.naja7i.profile.infrastructure.web.dto.CreateReviewRequest;
import ma.naja7i.profile.infrastructure.web.dto.ReviewView;
import ma.naja7i.profile.infrastructure.web.mapper.ProfileMappers;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.UserContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Reviews")
public class ReviewController {

    private final ProfReviewService reviewService;
    private final ProfileMappers mappers;

    @PostMapping("/me/reviews")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Create a review (1..5 stars)")
    public ApiResponse<ReviewView> create(@AuthUser UserContext ctx,
                                          @Valid @RequestBody CreateReviewRequest req) {
        // TODO: booking-service should gate this — verify CONFIRMEE booking exists for studentId+profId+bookingId.
        Review saved = reviewService.createReview(
                ctx.userId(), req.bookingId(), req.profId(), req.stars(), req.comment());
        return ApiResponse.of(mappers.toReviewView(saved));
    }
}

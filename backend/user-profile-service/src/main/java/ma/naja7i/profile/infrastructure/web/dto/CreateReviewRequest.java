package ma.naja7i.profile.infrastructure.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateReviewRequest(
        UUID bookingId,
        @NotNull UUID profId,
        @Min(1) @Max(5) int stars,
        @Size(max = 2000) String comment
) { }

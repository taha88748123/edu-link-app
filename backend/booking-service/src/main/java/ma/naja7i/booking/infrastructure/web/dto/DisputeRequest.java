package ma.naja7i.booking.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record DisputeRequest(
        @NotBlank String cause,
        String evidenceUrls
) {
}

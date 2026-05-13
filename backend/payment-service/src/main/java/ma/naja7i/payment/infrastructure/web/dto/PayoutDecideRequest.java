package ma.naja7i.payment.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PayoutDecideRequest(
        @NotBlank @Pattern(regexp = "APPROVE|REJECT") String decision,
        String notes
) {
}

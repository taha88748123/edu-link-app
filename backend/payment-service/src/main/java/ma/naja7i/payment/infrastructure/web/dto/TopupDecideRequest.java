package ma.naja7i.payment.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record TopupDecideRequest(
        @NotBlank @Pattern(regexp = "APPROVE|MODIFY|REJECT") String decision,
        BigDecimal approvedAmountMad,
        String notes
) {
}

package ma.naja7i.payment.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.naja7i.payment.domain.TopupMethod;

import java.math.BigDecimal;

public record CreateTopupRequest(
        @NotNull TopupMethod method,
        @NotNull @DecimalMin(value = "0.01", message = "amountMad must be positive") BigDecimal amountMad,
        @Size(max = 500) String proofUrl
) {
}

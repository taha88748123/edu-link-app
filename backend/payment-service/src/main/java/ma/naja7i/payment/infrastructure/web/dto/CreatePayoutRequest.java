package ma.naja7i.payment.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePayoutRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amountMad,
        @Size(max = 34) String ibanRef
) {
}

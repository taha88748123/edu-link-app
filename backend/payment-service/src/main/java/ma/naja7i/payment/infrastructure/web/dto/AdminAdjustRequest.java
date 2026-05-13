package ma.naja7i.payment.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdminAdjustRequest(
        @NotNull BigDecimal deltaMad,
        @NotBlank @Pattern(regexp = "ADMIN_CREDIT|ADMIN_DEBIT|ADMIN_ADJUST") String operation,
        @NotBlank @Size(max = 500) String reason
) {
}

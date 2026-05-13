package ma.naja7i.payment.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PayoutMarkPaidRequest(
        @NotBlank @Size(max = 100) String paymentRef
) {
}

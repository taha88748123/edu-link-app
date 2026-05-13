package ma.naja7i.booking.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Admin decision body for {@code POST /api/v1/bookings/{id}/disputes/{disputeId}:resolve}.
 *
 * <p>{@code decision} is one of {@code REFUND}, {@code PAY_PROF}, {@code SPLIT}.
 * For SPLIT, both {@code profShareMad} and {@code studentRefundMad} should be set.
 */
public record ResolveDisputeRequest(
        @NotBlank String decision,
        BigDecimal profShareMad,
        BigDecimal studentRefundMad,
        String notes
) {
    public static final String DECISION_REFUND  = "REFUND";
    public static final String DECISION_PAY_PROF = "PAY_PROF";
    public static final String DECISION_SPLIT   = "SPLIT";
}

package ma.naja7i.notification.infrastructure.messaging.incoming;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Consumer-side mirrors of the {@code payment-service} event payloads.
 */
public final class PaymentEvents {

    private PaymentEvents() {}

    /** {@code naja7i.payment.topup.submitted.v1} */
    public record TopupSubmitted(UUID topupId, UUID studentId, BigDecimal amountMad,
                                 String method, String proofUrl) {}

    /** {@code naja7i.payment.topup.decided.v1} */
    public record TopupDecided(UUID topupId, UUID studentId, String decision,
                               BigDecimal approvedAmountMad, UUID decidedBy) {}

    /** {@code naja7i.payment.payout.requested.v1} */
    public record PayoutRequested(UUID payoutId, UUID profId, BigDecimal amountMad) {}
}

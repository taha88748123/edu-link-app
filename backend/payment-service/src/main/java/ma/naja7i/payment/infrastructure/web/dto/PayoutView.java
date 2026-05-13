package ma.naja7i.payment.infrastructure.web.dto;

import ma.naja7i.payment.domain.Payout;

import java.time.Instant;
import java.util.UUID;

public record PayoutView(
        UUID id,
        UUID profId,
        long requestedAmountMinor,
        Long approvedAmountMinor,
        String currency,
        String status,
        String ibanRef,
        String paymentRef,
        String adminNotes,
        UUID decidedBy,
        Instant requestedAt,
        Instant decidedAt,
        Instant paidAt
) {

    public static PayoutView from(Payout p) {
        return new PayoutView(
                p.getId(),
                p.getProfId(),
                p.getRequestedAmountMinor(),
                p.getApprovedAmountMinor(),
                p.getCurrency(),
                p.getStatus() == null ? null : p.getStatus().name(),
                p.getIbanRef(),
                p.getPaymentRef(),
                p.getAdminNotes(),
                p.getDecidedBy(),
                p.getRequestedAt(),
                p.getDecidedAt(),
                p.getPaidAt()
        );
    }
}

package ma.naja7i.payment.infrastructure.web.dto;

import ma.naja7i.payment.domain.Topup;

import java.time.Instant;
import java.util.UUID;

public record TopupView(
        UUID id,
        UUID studentId,
        String method,
        long amountMinor,
        Long approvedAmountMinor,
        String currency,
        String proofUrl,
        String status,
        String adminNotes,
        UUID decidedBy,
        Instant decidedAt,
        Instant createdAt
) {

    public static TopupView from(Topup t) {
        return new TopupView(
                t.getId(),
                t.getStudentId(),
                t.getMethod() == null ? null : t.getMethod().name(),
                t.getAmountMinor(),
                t.getApprovedAmountMinor(),
                t.getCurrency(),
                t.getProofUrl(),
                t.getStatus() == null ? null : t.getStatus().name(),
                t.getAdminNotes(),
                t.getDecidedBy(),
                t.getDecidedAt(),
                t.getCreatedAt()
        );
    }
}

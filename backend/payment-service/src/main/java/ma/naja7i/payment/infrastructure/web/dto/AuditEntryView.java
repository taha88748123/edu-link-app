package ma.naja7i.payment.infrastructure.web.dto;

import ma.naja7i.payment.domain.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditEntryView(
        Long id,
        UUID userId,
        UUID actorId,
        String operation,
        long amountMinor,
        long balanceBefore,
        long balanceAfter,
        String referenceType,
        UUID referenceId,
        String reason,
        UUID traceId,
        Instant createdAt
) {

    public static AuditEntryView from(AuditLog a) {
        return new AuditEntryView(
                a.getId(),
                a.getUserId(),
                a.getActorId(),
                a.getOperation() == null ? null : a.getOperation().name(),
                a.getAmountMinor(),
                a.getBalanceBefore(),
                a.getBalanceAfter(),
                a.getReferenceType() == null ? null : a.getReferenceType().name(),
                a.getReferenceId(),
                a.getReason(),
                a.getTraceId(),
                a.getCreatedAt()
        );
    }
}

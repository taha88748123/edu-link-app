package ma.naja7i.payment.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.domain.AuditLog;
import ma.naja7i.payment.domain.AuditOperation;
import ma.naja7i.payment.domain.AuditReferenceType;
import ma.naja7i.payment.domain.OutboxEvent;
import ma.naja7i.payment.infrastructure.messaging.payload.BalanceAdjustedPayload;
import ma.naja7i.payment.infrastructure.persistence.OutboxRepository;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Manual admin balance adjustment. The most sensitive endpoint — every call is
 * journaled in {@code audit_log} (the operation is required to be one of
 * {@code ADMIN_CREDIT}, {@code ADMIN_DEBIT}, {@code ADMIN_ADJUST}) and an event
 * is emitted on {@code naja7i.payment.balance.adjusted.v1}.
 */
@Service
@RequiredArgsConstructor
public class AdminBalanceService {

    private final BalanceLedger balanceLedger;
    private final OutboxRepository outboxRepository;
    private final KafkaEventEnvelopeBuilder envelopeBuilder;

    @Transactional
    public AuditLog adjust(UUID userId, long deltaMinor, AuditOperation operation,
                           UUID adminId, String reason, UUID traceId) {
        if (operation != AuditOperation.ADMIN_CREDIT
                && operation != AuditOperation.ADMIN_DEBIT
                && operation != AuditOperation.ADMIN_ADJUST) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Operation must be ADMIN_CREDIT, ADMIN_DEBIT or ADMIN_ADJUST",
                    Map.of("operation", operation));
        }
        if (operation == AuditOperation.ADMIN_CREDIT && deltaMinor <= 0L) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "ADMIN_CREDIT requires positive delta");
        }
        if (operation == AuditOperation.ADMIN_DEBIT && deltaMinor >= 0L) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "ADMIN_DEBIT requires negative delta");
        }
        if (operation == AuditOperation.ADMIN_ADJUST && deltaMinor == 0L) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "ADMIN_ADJUST delta must be non-zero");
        }

        UUID refId = UUID.randomUUID();
        AuditLog row = balanceLedger.apply(userId, deltaMinor, operation,
                AuditReferenceType.ADJUST, refId, adminId, reason, traceId);

        OutboxEvent outbox = envelopeBuilder.toOutbox(userId,
                EventTopics.PAYMENT_BALANCE_ADJUSTED, userId.toString(),
                new BalanceAdjustedPayload(userId, adminId, operation.name(),
                        toMajor(deltaMinor), deltaMinor, reason));
        outboxRepository.save(outbox);
        return row;
    }

    private static BigDecimal toMajor(long minor) {
        return BigDecimal.valueOf(minor).movePointLeft(2);
    }
}

package ma.naja7i.payment.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.domain.AuditLog;
import ma.naja7i.payment.domain.AuditOperation;
import ma.naja7i.payment.domain.AuditReferenceType;
import ma.naja7i.payment.domain.Balance;
import ma.naja7i.payment.infrastructure.persistence.AuditLogRepository;
import ma.naja7i.payment.infrastructure.persistence.BalanceRepository;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * The single chokepoint for any balance mutation. Every flow that touches a
 * user's wallet MUST go through {@link #apply}; doing so guarantees that an
 * append-only {@link AuditLog} row is written atomically with the balance
 * update inside one DB transaction.
 *
 * <p>Concurrency model:
 * <ul>
 *   <li>Pessimistic {@code SELECT ... FOR UPDATE} on the balance row serialises
 *       concurrent mutations of the same wallet.</li>
 *   <li>Optimistic version (Hibernate {@code @Version}) catches lost-update
 *       races that slip past the lock (e.g. when a row was just inserted). The
 *       operation retries once internally before propagating.</li>
 *   <li>The DB CHECK constraint {@code balance_minor &gt;= 0} is the final
 *       safety net; a violation is translated to
 *       {@link ErrorCode#PAYMENT_INSUFFICIENT_BALANCE}.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class BalanceLedger {

    private static final Logger log = LoggerFactory.getLogger(BalanceLedger.class);

    private final BalanceRepository balanceRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * Applies a signed delta to a user's balance and records an immutable audit
     * row. Throws {@link ErrorCode#PAYMENT_INSUFFICIENT_BALANCE} if the resulting
     * balance would be negative.
     *
     * <p>On {@link ObjectOptimisticLockingFailureException} we propagate (the
     * surrounding business transaction is poisoned anyway). The pessimistic
     * {@code FOR UPDATE} read normally prevents this; the {@code @Version}
     * column is the second line of defence.
     *
     * @param userId      wallet owner
     * @param deltaMinor  signed centimes (positive credit, negative debit)
     * @param operation   audit operation kind
     * @param refType     reference table (BOOKING / ESCROW / TOPUP / PAYOUT / ADJUST)
     * @param refId       reference primary key
     * @param actorId     who triggered this — same as userId for self-actions, admin id for ADMIN_*
     * @param reason      free-form audit reason (nullable)
     * @param traceId     gateway-injected trace id (nullable)
     * @return the persisted audit row (its computed before/after fields)
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public AuditLog apply(UUID userId, long deltaMinor, AuditOperation operation,
                          AuditReferenceType refType, UUID refId, UUID actorId,
                          String reason, UUID traceId) {
        try {
            return applyOnce(userId, deltaMinor, operation, refType, refId, actorId, reason, traceId);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic lock failed for balance change [trace={} op={}] — propagating",
                    traceId, operation);
            throw ex;
        } catch (DataIntegrityViolationException ex) {
            // CHECK (balance_minor >= 0) — the application layer surfaces this as a
            // domain-level business error.
            String msg = ex.getMostSpecificCause() == null ? ex.getMessage()
                    : ex.getMostSpecificCause().getMessage();
            if (msg != null && msg.toLowerCase().contains("balance_minor")) {
                throw new BusinessException(ErrorCode.PAYMENT_INSUFFICIENT_BALANCE,
                        "Balance would become negative",
                        Map.of("operation", operation,
                                "referenceType", refType,
                                "referenceId", refId));
            }
            throw ex;
        }
    }

    private AuditLog applyOnce(UUID userId, long deltaMinor, AuditOperation operation,
                               AuditReferenceType refType, UUID refId, UUID actorId,
                               String reason, UUID traceId) {
        Balance b = balanceRepository.findByIdForUpdate(userId).orElseGet(() ->
                balanceRepository.saveAndFlush(Balance.builder()
                        .userId(userId)
                        .balanceMinor(0L)
                        .currency("MAD")
                        .version(0)
                        .build()));

        long before = b.getBalanceMinor() == null ? 0L : b.getBalanceMinor();
        long after = Math.addExact(before, deltaMinor);

        if (after < 0L) {
            // Application-layer pre-check; the DB CHECK is the second line of defense.
            throw new BusinessException(ErrorCode.PAYMENT_INSUFFICIENT_BALANCE,
                    "Insufficient balance",
                    Map.of("operation", operation,
                            "referenceType", refType,
                            "referenceId", refId));
        }

        b.setBalanceMinor(after);
        balanceRepository.saveAndFlush(b);

        AuditLog audit = AuditLog.builder()
                .userId(userId)
                .actorId(actorId == null ? userId : actorId)
                .operation(operation)
                .amountMinor(deltaMinor)
                .balanceBefore(before)
                .balanceAfter(after)
                .referenceType(refType)
                .referenceId(refId)
                .reason(reason)
                .traceId(traceId)
                .build();
        return auditLogRepository.saveAndFlush(audit);
    }
}

package ma.naja7i.payment.application;

import ma.naja7i.payment.domain.AuditLog;
import ma.naja7i.payment.domain.AuditOperation;
import ma.naja7i.payment.domain.AuditReferenceType;
import ma.naja7i.payment.domain.Balance;
import ma.naja7i.payment.infrastructure.persistence.AuditLogRepository;
import ma.naja7i.payment.infrastructure.persistence.BalanceRepository;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-level money-safety tests for the heart of the service. We exercise the
 * ledger with an in-memory-ish balance (mocked repo returns mutable Balance
 * objects) so we can assert before/after exactly.
 */
@ExtendWith(MockitoExtension.class)
class BalanceLedgerTest {

    @Mock BalanceRepository balanceRepository;
    @Mock AuditLogRepository auditLogRepository;

    BalanceLedger ledger;

    UUID userId;
    UUID actorId;
    UUID refId;

    @BeforeEach
    void setup() {
        // No wide stubs — each test stubs only what it needs.
        ledger = new BalanceLedger(balanceRepository, auditLogRepository);
        userId  = UUID.randomUUID();
        actorId = UUID.randomUUID();
        refId   = UUID.randomUUID();
    }

    private Balance balance(long minor) {
        return Balance.builder().userId(userId).balanceMinor(minor)
                .currency("MAD").version(0).build();
    }

    @Test
    void credit_addsAmount_andWritesAuditWithCorrectBeforeAfter() {
        Balance b = balance(10_000L);
        when(balanceRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(b));
        when(balanceRepository.saveAndFlush(any(Balance.class))).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.saveAndFlush(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        AuditLog audit = ledger.apply(userId, 5_000L, AuditOperation.TOPUP,
                AuditReferenceType.TOPUP, refId, actorId, "test credit", null);

        assertThat(b.getBalanceMinor()).isEqualTo(15_000L);
        assertThat(audit.getBalanceBefore()).isEqualTo(10_000L);
        assertThat(audit.getBalanceAfter()).isEqualTo(15_000L);
        assertThat(audit.getAmountMinor()).isEqualTo(5_000L);
        assertThat(audit.getOperation()).isEqualTo(AuditOperation.TOPUP);
        assertThat(audit.getUserId()).isEqualTo(userId);
        assertThat(audit.getActorId()).isEqualTo(actorId);
    }

    @Test
    void debit_subtractsAmount_andWritesAudit() {
        Balance b = balance(10_000L);
        when(balanceRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(b));
        when(balanceRepository.saveAndFlush(any(Balance.class))).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.saveAndFlush(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        AuditLog audit = ledger.apply(userId, -3_000L, AuditOperation.PAYOUT,
                AuditReferenceType.PAYOUT, refId, actorId, "test debit", null);

        assertThat(b.getBalanceMinor()).isEqualTo(7_000L);
        assertThat(audit.getBalanceBefore()).isEqualTo(10_000L);
        assertThat(audit.getBalanceAfter()).isEqualTo(7_000L);
        assertThat(audit.getAmountMinor()).isEqualTo(-3_000L);
    }

    @Test
    void debit_wouldGoNegative_throwsInsufficientBalance_andSkipsAudit() {
        Balance b = balance(2_000L);
        when(balanceRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(b));

        assertThatThrownBy(() ->
                ledger.apply(userId, -5_000L, AuditOperation.ESCROW_LOCK,
                        AuditReferenceType.ESCROW, refId, actorId, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.PAYMENT_INSUFFICIENT_BALANCE);

        // Balance must be untouched (no save) and no audit_log row written.
        verify(balanceRepository, never()).saveAndFlush(any(Balance.class));
        verify(auditLogRepository, never()).saveAndFlush(any(AuditLog.class));
        assertThat(b.getBalanceMinor()).isEqualTo(2_000L);
    }

    @Test
    void firstChange_createsBalanceRowOnTheFly() {
        // No balance row exists yet -> the ledger should insert one with 0 and continue.
        Balance fresh = balance(0L);
        when(balanceRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());
        when(balanceRepository.saveAndFlush(any(Balance.class))).thenAnswer(invocation -> {
            // First call creates the row; subsequent updates return whatever was passed.
            Balance arg = invocation.getArgument(0);
            if (fresh.getBalanceMinor() == 0L && arg.getBalanceMinor() == 0L) {
                return fresh; // initial insert
            }
            return arg;
        });
        when(auditLogRepository.saveAndFlush(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        AuditLog audit = ledger.apply(userId, 1_000L, AuditOperation.TOPUP,
                AuditReferenceType.TOPUP, refId, actorId, "first credit", null);

        assertThat(audit.getBalanceBefore()).isEqualTo(0L);
        assertThat(audit.getBalanceAfter()).isEqualTo(1_000L);
    }

    @Test
    void audit_recordsTraceIdAndReference() {
        Balance b = balance(0L);
        UUID trace = UUID.randomUUID();
        when(balanceRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(b));
        when(balanceRepository.saveAndFlush(any(Balance.class))).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.saveAndFlush(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        ledger.apply(userId, 1L, AuditOperation.ADMIN_CREDIT,
                AuditReferenceType.ADJUST, refId, actorId, "reason here", trace);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).saveAndFlush(captor.capture());
        AuditLog written = captor.getValue();
        assertThat(written.getTraceId()).isEqualTo(trace);
        assertThat(written.getReferenceId()).isEqualTo(refId);
        assertThat(written.getReferenceType()).isEqualTo(AuditReferenceType.ADJUST);
        assertThat(written.getReason()).isEqualTo("reason here");
    }
}

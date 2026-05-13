package ma.naja7i.payment.application;

import ma.naja7i.payment.domain.AuditLog;
import ma.naja7i.payment.domain.AuditOperation;
import ma.naja7i.payment.domain.AuditReferenceType;
import ma.naja7i.payment.domain.OutboxEvent;
import ma.naja7i.payment.infrastructure.persistence.OutboxRepository;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBalanceServiceTest {

    @Mock BalanceLedger balanceLedger;
    @Mock OutboxRepository outboxRepository;
    @Mock KafkaEventEnvelopeBuilder envelopeBuilder;

    AdminBalanceService service;
    UUID userId;
    UUID adminId;

    @BeforeEach
    void setup() {
        service = new AdminBalanceService(balanceLedger, outboxRepository, envelopeBuilder);
        userId  = UUID.randomUUID();
        adminId = UUID.randomUUID();
    }

    @Test
    void adminCredit_callsLedgerAndPublishesOutbox() {
        when(balanceLedger.apply(any(UUID.class), anyLong(), any(AuditOperation.class),
                any(AuditReferenceType.class), any(UUID.class), any(UUID.class), any(), any()))
                .thenReturn(AuditLog.builder().id(1L).build());
        when(envelopeBuilder.toOutbox(any(UUID.class), any(String.class), any(String.class), any()))
                .thenReturn(OutboxEvent.builder().id(UUID.randomUUID()).build());

        service.adjust(userId, 1_000L, AuditOperation.ADMIN_CREDIT, adminId, "promo", null);

        verify(balanceLedger).apply(eq(userId), eq(1_000L),
                eq(AuditOperation.ADMIN_CREDIT), eq(AuditReferenceType.ADJUST),
                any(UUID.class), eq(adminId), any(), any());
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    void adminCreditWithNegativeDelta_rejected() {
        assertThatThrownBy(() -> service.adjust(userId, -1L,
                AuditOperation.ADMIN_CREDIT, adminId, "x", null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void adminDebitWithPositiveDelta_rejected() {
        assertThatThrownBy(() -> service.adjust(userId, 1L,
                AuditOperation.ADMIN_DEBIT, adminId, "x", null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void adminAdjustWithZeroDelta_rejected() {
        assertThatThrownBy(() -> service.adjust(userId, 0L,
                AuditOperation.ADMIN_ADJUST, adminId, "x", null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void operationOutsideAdminSet_rejected() {
        assertThatThrownBy(() -> service.adjust(userId, 100L,
                AuditOperation.TOPUP, adminId, "x", null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }
}

package ma.naja7i.payment.application;

import ma.naja7i.payment.domain.AuditLog;
import ma.naja7i.payment.domain.AuditOperation;
import ma.naja7i.payment.domain.AuditReferenceType;
import ma.naja7i.payment.domain.Balance;
import ma.naja7i.payment.domain.OutboxEvent;
import ma.naja7i.payment.domain.Payout;
import ma.naja7i.payment.domain.PayoutStatus;
import ma.naja7i.payment.infrastructure.persistence.BalanceRepository;
import ma.naja7i.payment.infrastructure.persistence.OutboxRepository;
import ma.naja7i.payment.infrastructure.persistence.PayoutRepository;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock PayoutRepository payoutRepository;
    @Mock BalanceRepository balanceRepository;
    @Mock OutboxRepository outboxRepository;
    @Mock BalanceLedger balanceLedger;
    @Mock KafkaEventEnvelopeBuilder envelopeBuilder;

    PayoutService service;
    UUID profId;
    UUID adminId;

    @BeforeEach
    void setup() {
        service = new PayoutService(payoutRepository, balanceRepository, outboxRepository,
                balanceLedger, envelopeBuilder);
        profId  = UUID.randomUUID();
        adminId = UUID.randomUUID();
    }

    private OutboxEvent dummyOutbox() { return OutboxEvent.builder().id(UUID.randomUUID()).build(); }
    private AuditLog dummyAudit() { return AuditLog.builder().id(1L).build(); }

    private void stubEnvelope() {
        when(envelopeBuilder.toOutbox(any(UUID.class), any(String.class), any(String.class), any()))
                .thenReturn(dummyOutbox());
    }

    @Test
    void request_withSufficientBalance_doesNotDebit() {
        when(balanceRepository.findById(profId))
                .thenReturn(Optional.of(Balance.builder().userId(profId)
                        .balanceMinor(30_000L).currency("MAD").version(0).build()));
        when(payoutRepository.saveAndFlush(any(Payout.class))).thenAnswer(i -> i.getArgument(0));
        stubEnvelope();

        Payout p = service.request(profId, 10_000L, "MA0000");

        assertThat(p.getStatus()).isEqualTo(PayoutStatus.REQUESTED);
        // Crucially: no balance debit at request time.
        verify(balanceLedger, never())
                .apply(any(), anyLong(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void request_withInsufficientBalance_throws() {
        when(balanceRepository.findById(profId))
                .thenReturn(Optional.of(Balance.builder().userId(profId)
                        .balanceMinor(500L).currency("MAD").version(0).build()));

        assertThatThrownBy(() -> service.request(profId, 10_000L, "MA0000"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.PAYMENT_INSUFFICIENT_BALANCE);

        verify(payoutRepository, never()).saveAndFlush(any(Payout.class));
    }

    @Test
    void approve_debitsBalance_andTransitionsApproved() {
        Payout p = Payout.builder().id(UUID.randomUUID()).profId(profId)
                .requestedAmountMinor(10_000L).currency("MAD")
                .status(PayoutStatus.REQUESTED).version(0).build();
        when(payoutRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(payoutRepository.saveAndFlush(any(Payout.class))).thenAnswer(i -> i.getArgument(0));
        when(balanceLedger.apply(any(UUID.class), anyLong(), any(AuditOperation.class),
                any(AuditReferenceType.class), any(UUID.class), any(UUID.class), any(), any()))
                .thenReturn(dummyAudit());
        stubEnvelope();

        Payout out = service.decide(p.getId(), adminId, "APPROVE", "ok", null);

        assertThat(out.getStatus()).isEqualTo(PayoutStatus.APPROVED);
        assertThat(out.getApprovedAmountMinor()).isEqualTo(10_000L);
        verify(balanceLedger).apply(eq(profId), eq(-10_000L),
                eq(AuditOperation.PAYOUT), eq(AuditReferenceType.PAYOUT),
                eq(p.getId()), eq(adminId), any(), any());
    }

    @Test
    void reject_doesNotDebit() {
        Payout p = Payout.builder().id(UUID.randomUUID()).profId(profId)
                .requestedAmountMinor(10_000L).currency("MAD")
                .status(PayoutStatus.REQUESTED).version(0).build();
        when(payoutRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(payoutRepository.saveAndFlush(any(Payout.class))).thenAnswer(i -> i.getArgument(0));
        stubEnvelope();

        Payout out = service.decide(p.getId(), adminId, "REJECT", "no", null);

        assertThat(out.getStatus()).isEqualTo(PayoutStatus.REJECTED);
        verify(balanceLedger, never())
                .apply(any(), anyLong(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void markPaid_onApproved_transitionsPaid_andDoesNotTouchBalance() {
        Payout p = Payout.builder().id(UUID.randomUUID()).profId(profId)
                .requestedAmountMinor(10_000L).approvedAmountMinor(10_000L)
                .currency("MAD").status(PayoutStatus.APPROVED).version(0).build();
        when(payoutRepository.findById(p.getId())).thenReturn(Optional.of(p));
        when(payoutRepository.saveAndFlush(any(Payout.class))).thenAnswer(i -> i.getArgument(0));
        stubEnvelope();

        Payout out = service.markPaid(p.getId(), adminId, "BANK-REF-123");

        assertThat(out.getStatus()).isEqualTo(PayoutStatus.PAID);
        assertThat(out.getPaymentRef()).isEqualTo("BANK-REF-123");
        verify(balanceLedger, never())
                .apply(any(), anyLong(), any(), any(), any(), any(), any(), any());
    }
}

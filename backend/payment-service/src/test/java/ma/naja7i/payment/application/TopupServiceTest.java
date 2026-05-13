package ma.naja7i.payment.application;

import ma.naja7i.payment.domain.AuditLog;
import ma.naja7i.payment.domain.AuditOperation;
import ma.naja7i.payment.domain.AuditReferenceType;
import ma.naja7i.payment.domain.OutboxEvent;
import ma.naja7i.payment.domain.Topup;
import ma.naja7i.payment.domain.TopupMethod;
import ma.naja7i.payment.domain.TopupStatus;
import ma.naja7i.payment.infrastructure.persistence.OutboxRepository;
import ma.naja7i.payment.infrastructure.persistence.TopupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopupServiceTest {

    @Mock TopupRepository topupRepository;
    @Mock OutboxRepository outboxRepository;
    @Mock BalanceLedger balanceLedger;
    @Mock KafkaEventEnvelopeBuilder envelopeBuilder;

    TopupService service;
    UUID studentId;
    UUID adminId;

    @BeforeEach
    void setup() {
        service = new TopupService(topupRepository, outboxRepository, balanceLedger, envelopeBuilder);
        studentId = UUID.randomUUID();
        adminId   = UUID.randomUUID();
    }

    private OutboxEvent dummyOutbox() { return OutboxEvent.builder().id(UUID.randomUUID()).build(); }
    private AuditLog dummyAudit() { return AuditLog.builder().id(1L).build(); }

    private void stubEnvelope() {
        when(envelopeBuilder.toOutbox(any(UUID.class), any(String.class), any(String.class), any()))
                .thenReturn(dummyOutbox());
    }

    @Test
    void approve_creditsRequestedAmount_andTransitionsApproved() {
        Topup t = Topup.builder().id(UUID.randomUUID()).studentId(studentId)
                .method(TopupMethod.CASHPLUS).amountMinor(20_000L).currency("MAD")
                .status(TopupStatus.EN_ATTENTE_ADMIN).version(0).build();
        when(topupRepository.findById(t.getId())).thenReturn(Optional.of(t));
        when(topupRepository.saveAndFlush(any(Topup.class))).thenAnswer(i -> i.getArgument(0));
        when(balanceLedger.apply(any(UUID.class), anyLong(), any(AuditOperation.class),
                any(AuditReferenceType.class), any(UUID.class), any(UUID.class), any(), any()))
                .thenReturn(dummyAudit());
        stubEnvelope();

        Topup out = service.decide(t.getId(), adminId, "APPROVE", null, "ok", null);

        assertThat(out.getStatus()).isEqualTo(TopupStatus.APPROVED);
        assertThat(out.getApprovedAmountMinor()).isEqualTo(20_000L);
        verify(balanceLedger).apply(eq(studentId), eq(20_000L),
                eq(AuditOperation.TOPUP), eq(AuditReferenceType.TOPUP),
                eq(t.getId()), eq(adminId), any(), any());
    }

    @Test
    void modify_creditsModifiedAmount() {
        Topup t = Topup.builder().id(UUID.randomUUID()).studentId(studentId)
                .method(TopupMethod.BANK_TRANSFER).amountMinor(20_000L).currency("MAD")
                .status(TopupStatus.EN_ATTENTE_ADMIN).version(0).build();
        when(topupRepository.findById(t.getId())).thenReturn(Optional.of(t));
        when(topupRepository.saveAndFlush(any(Topup.class))).thenAnswer(i -> i.getArgument(0));
        when(balanceLedger.apply(any(UUID.class), anyLong(), any(AuditOperation.class),
                any(AuditReferenceType.class), any(UUID.class), any(UUID.class), any(), any()))
                .thenReturn(dummyAudit());
        stubEnvelope();

        Topup out = service.decide(t.getId(), adminId, "MODIFY", 15_000L, "verified less", null);

        assertThat(out.getStatus()).isEqualTo(TopupStatus.MODIFIED);
        assertThat(out.getApprovedAmountMinor()).isEqualTo(15_000L);
        verify(balanceLedger).apply(eq(studentId), eq(15_000L),
                eq(AuditOperation.TOPUP), eq(AuditReferenceType.TOPUP),
                eq(t.getId()), eq(adminId), any(), any());
    }

    @Test
    void reject_doesNotCreditBalance() {
        Topup t = Topup.builder().id(UUID.randomUUID()).studentId(studentId)
                .method(TopupMethod.CASHPLUS).amountMinor(20_000L).currency("MAD")
                .status(TopupStatus.EN_ATTENTE_ADMIN).version(0).build();
        when(topupRepository.findById(t.getId())).thenReturn(Optional.of(t));
        when(topupRepository.saveAndFlush(any(Topup.class))).thenAnswer(i -> i.getArgument(0));
        stubEnvelope();

        Topup out = service.decide(t.getId(), adminId, "REJECT", null, "no proof", null);

        assertThat(out.getStatus()).isEqualTo(TopupStatus.REJECTED);
        assertThat(out.getApprovedAmountMinor()).isEqualTo(0L);
        verify(balanceLedger, never())
                .apply(any(), anyLong(), any(), any(), any(), any(), any(), any());
    }
}

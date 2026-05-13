package ma.naja7i.payment.application;

import ma.naja7i.payment.config.PaymentProperties;
import ma.naja7i.payment.domain.AuditLog;
import ma.naja7i.payment.domain.AuditOperation;
import ma.naja7i.payment.domain.AuditReferenceType;
import ma.naja7i.payment.domain.Escrow;
import ma.naja7i.payment.domain.EscrowStatus;
import ma.naja7i.payment.domain.OutboxEvent;
import ma.naja7i.payment.infrastructure.persistence.EscrowRepository;
import ma.naja7i.payment.infrastructure.persistence.OutboxRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscrowServiceTest {

    @Mock EscrowRepository escrowRepository;
    @Mock OutboxRepository outboxRepository;
    @Mock BalanceLedger balanceLedger;
    @Mock KafkaEventEnvelopeBuilder envelopeBuilder;

    EscrowService service;
    PaymentProperties properties;

    UUID studentId;
    UUID profId;
    UUID bookingId;
    UUID traceId;

    @BeforeEach
    void setup() {
        properties = new PaymentProperties();
        // 10% platform fee — keeps amounts easy to read in tests.
        properties.setPlatformFeeBps(1_000);
        service = new EscrowService(escrowRepository, outboxRepository,
                balanceLedger, envelopeBuilder, properties);
        studentId = UUID.randomUUID();
        profId    = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        traceId   = UUID.randomUUID();
    }

    private OutboxEvent dummyOutbox() {
        return OutboxEvent.builder().id(UUID.randomUUID()).build();
    }

    private AuditLog dummyAudit() {
        return AuditLog.builder().id(1L).build();
    }

    private void stubLedgerSuccess() {
        when(balanceLedger.apply(any(UUID.class), anyLong(), any(AuditOperation.class),
                any(AuditReferenceType.class), any(UUID.class),
                any(UUID.class), any(), any()))
                .thenReturn(dummyAudit());
    }

    private void stubEnvelope() {
        when(envelopeBuilder.toOutbox(any(UUID.class), any(String.class), any(String.class), any()))
                .thenReturn(dummyOutbox());
    }

    // ============================================================ onBookingRequested

    @Test
    void onBookingRequested_locksAmount_andEmitsEvent() {
        when(escrowRepository.existsByBookingId(bookingId)).thenReturn(false);
        when(escrowRepository.saveAndFlush(any(Escrow.class))).thenAnswer(i -> i.getArgument(0));
        stubLedgerSuccess();
        stubEnvelope();

        service.onBookingRequested(bookingId, studentId, profId, 15_000L, traceId);

        // 10% fee → 1500 fee, 13500 prof share. Ledger debits student by -15000.
        verify(balanceLedger).apply(eq(studentId), eq(-15_000L),
                eq(AuditOperation.ESCROW_LOCK), eq(AuditReferenceType.ESCROW),
                any(UUID.class), eq(studentId), any(), eq(traceId));

        ArgumentCaptor<Escrow> escrowCaptor = ArgumentCaptor.forClass(Escrow.class);
        verify(escrowRepository).saveAndFlush(escrowCaptor.capture());
        Escrow saved = escrowCaptor.getValue();
        assertThat(saved.getAmountMinor()).isEqualTo(15_000L);
        assertThat(saved.getPlatformFeeMinor()).isEqualTo(1_500L);
        assertThat(saved.getProfShareMinor()).isEqualTo(13_500L);
        assertThat(saved.getStatus()).isEqualTo(EscrowStatus.LOCKED);
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    void onBookingRequested_existingEscrow_isIdempotent_andSkips() {
        when(escrowRepository.existsByBookingId(bookingId)).thenReturn(true);

        service.onBookingRequested(bookingId, studentId, profId, 15_000L, traceId);

        verify(balanceLedger, never()).apply(any(), anyLong(), any(), any(), any(), any(), any(), any());
        verify(escrowRepository, never()).saveAndFlush(any(Escrow.class));
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void onBookingRequested_insufficientBalance_doesNotCreateEscrow() {
        when(escrowRepository.existsByBookingId(bookingId)).thenReturn(false);
        when(balanceLedger.apply(any(UUID.class), anyLong(), any(AuditOperation.class),
                any(AuditReferenceType.class), any(UUID.class), any(UUID.class), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.PAYMENT_INSUFFICIENT_BALANCE,
                        "not enough"));

        service.onBookingRequested(bookingId, studentId, profId, 15_000L, traceId);

        verify(escrowRepository, never()).saveAndFlush(any(Escrow.class));
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    // ============================================================ onBookingConfirmed

    @Test
    void onBookingConfirmed_releasesProfShare() {
        Escrow e = Escrow.builder()
                .id(UUID.randomUUID()).bookingId(bookingId)
                .studentId(studentId).profId(profId)
                .amountMinor(15_000L).platformFeeMinor(1_500L).profShareMinor(13_500L)
                .status(EscrowStatus.LOCKED).currency("MAD").version(0)
                .build();
        when(escrowRepository.findByBookingId(bookingId)).thenReturn(Optional.of(e));
        when(escrowRepository.saveAndFlush(any(Escrow.class))).thenAnswer(i -> i.getArgument(0));
        stubLedgerSuccess();
        stubEnvelope();

        service.onBookingConfirmed(bookingId, traceId);

        verify(balanceLedger).apply(eq(profId), eq(13_500L),
                eq(AuditOperation.ESCROW_RELEASE), eq(AuditReferenceType.ESCROW),
                eq(e.getId()), eq(profId), any(), eq(traceId));
        assertThat(e.getStatus()).isEqualTo(EscrowStatus.RELEASED_TO_PROF);
        assertThat(e.getProfPayoutMinor()).isEqualTo(13_500L);
    }

    @Test
    void onBookingConfirmed_alreadyReleased_isNoOp() {
        Escrow e = Escrow.builder()
                .id(UUID.randomUUID()).bookingId(bookingId)
                .studentId(studentId).profId(profId)
                .amountMinor(10_000L).platformFeeMinor(1_000L).profShareMinor(9_000L)
                .status(EscrowStatus.RELEASED_TO_PROF).currency("MAD")
                .build();
        when(escrowRepository.findByBookingId(bookingId)).thenReturn(Optional.of(e));

        service.onBookingConfirmed(bookingId, traceId);

        verify(balanceLedger, never()).apply(any(), anyLong(), any(), any(), any(), any(), any(), any());
    }

    // ============================================================ onBookingCancelled

    @Test
    void onBookingCancelled_refundsFullAmount() {
        Escrow e = Escrow.builder()
                .id(UUID.randomUUID()).bookingId(bookingId)
                .studentId(studentId).profId(profId)
                .amountMinor(20_000L).platformFeeMinor(2_000L).profShareMinor(18_000L)
                .status(EscrowStatus.LOCKED).currency("MAD")
                .build();
        when(escrowRepository.findByBookingId(bookingId)).thenReturn(Optional.of(e));
        when(escrowRepository.saveAndFlush(any(Escrow.class))).thenAnswer(i -> i.getArgument(0));
        stubLedgerSuccess();
        stubEnvelope();

        service.onBookingCancelled(bookingId, traceId, "STUDENT_CHANGED_MIND");

        verify(balanceLedger).apply(eq(studentId), eq(20_000L),
                eq(AuditOperation.REFUND), eq(AuditReferenceType.ESCROW),
                eq(e.getId()), eq(studentId), any(), eq(traceId));
        assertThat(e.getStatus()).isEqualTo(EscrowStatus.REFUNDED_TO_STUDENT);
        assertThat(e.getStudentRefundMinor()).isEqualTo(20_000L);
    }

    // ============================================================ onDisputeResolved

    @Test
    void onDisputeResolved_split_distributesCorrectly() {
        Escrow e = Escrow.builder()
                .id(UUID.randomUUID()).bookingId(bookingId)
                .studentId(studentId).profId(profId)
                .amountMinor(10_000L).platformFeeMinor(1_000L).profShareMinor(9_000L)
                .status(EscrowStatus.LOCKED).currency("MAD")
                .build();
        when(escrowRepository.findByBookingId(bookingId)).thenReturn(Optional.of(e));
        when(escrowRepository.saveAndFlush(any(Escrow.class))).thenAnswer(i -> i.getArgument(0));
        stubLedgerSuccess();
        stubEnvelope();

        service.onDisputeResolved(bookingId, "RESOLVED_SPLIT", 4_000L, 6_000L, traceId);

        verify(balanceLedger).apply(eq(profId), eq(4_000L),
                eq(AuditOperation.ESCROW_RELEASE), eq(AuditReferenceType.ESCROW),
                eq(e.getId()), eq(profId), any(), eq(traceId));
        verify(balanceLedger).apply(eq(studentId), eq(6_000L),
                eq(AuditOperation.REFUND), eq(AuditReferenceType.ESCROW),
                eq(e.getId()), eq(studentId), any(), eq(traceId));
        assertThat(e.getStatus()).isEqualTo(EscrowStatus.SPLIT);
        assertThat(e.getProfPayoutMinor()).isEqualTo(4_000L);
        assertThat(e.getStudentRefundMinor()).isEqualTo(6_000L);
    }

    @Test
    void onDisputeResolved_split_amountsMustSumToEscrowTotal() {
        Escrow e = Escrow.builder()
                .id(UUID.randomUUID()).bookingId(bookingId)
                .studentId(studentId).profId(profId)
                .amountMinor(10_000L).platformFeeMinor(1_000L).profShareMinor(9_000L)
                .status(EscrowStatus.LOCKED).currency("MAD")
                .build();
        when(escrowRepository.findByBookingId(bookingId)).thenReturn(Optional.of(e));

        assertThatThrownBy(() ->
                service.onDisputeResolved(bookingId, "RESOLVED_SPLIT", 3_000L, 5_000L, traceId))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void onDisputeResolved_payProf_creditsProfShare() {
        Escrow e = Escrow.builder()
                .id(UUID.randomUUID()).bookingId(bookingId)
                .studentId(studentId).profId(profId)
                .amountMinor(10_000L).platformFeeMinor(1_000L).profShareMinor(9_000L)
                .status(EscrowStatus.LOCKED).currency("MAD")
                .build();
        when(escrowRepository.findByBookingId(bookingId)).thenReturn(Optional.of(e));
        when(escrowRepository.saveAndFlush(any(Escrow.class))).thenAnswer(i -> i.getArgument(0));
        stubLedgerSuccess();
        stubEnvelope();

        service.onDisputeResolved(bookingId, "RESOLVED_PAY_PROF", null, null, traceId);

        verify(balanceLedger).apply(eq(profId), eq(9_000L),
                eq(AuditOperation.ESCROW_RELEASE), eq(AuditReferenceType.ESCROW),
                eq(e.getId()), eq(profId), any(), eq(traceId));
        assertThat(e.getStatus()).isEqualTo(EscrowStatus.RELEASED_TO_PROF);
    }

    @Test
    void onDisputeResolved_refund_refundsFullAmount() {
        Escrow e = Escrow.builder()
                .id(UUID.randomUUID()).bookingId(bookingId)
                .studentId(studentId).profId(profId)
                .amountMinor(10_000L).platformFeeMinor(1_000L).profShareMinor(9_000L)
                .status(EscrowStatus.LOCKED).currency("MAD")
                .build();
        when(escrowRepository.findByBookingId(bookingId)).thenReturn(Optional.of(e));
        when(escrowRepository.saveAndFlush(any(Escrow.class))).thenAnswer(i -> i.getArgument(0));
        stubLedgerSuccess();
        stubEnvelope();

        service.onDisputeResolved(bookingId, "RESOLVED_REFUND", null, null, traceId);

        verify(balanceLedger).apply(eq(studentId), eq(10_000L),
                eq(AuditOperation.REFUND), eq(AuditReferenceType.ESCROW),
                eq(e.getId()), eq(studentId), any(), eq(traceId));
        assertThat(e.getStatus()).isEqualTo(EscrowStatus.REFUNDED_TO_STUDENT);
    }
}

package ma.naja7i.payment.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.payment.domain.AuditOperation;
import ma.naja7i.payment.domain.AuditReferenceType;
import ma.naja7i.payment.domain.OutboxEvent;
import ma.naja7i.payment.domain.Topup;
import ma.naja7i.payment.domain.TopupMethod;
import ma.naja7i.payment.domain.TopupStatus;
import ma.naja7i.payment.infrastructure.messaging.payload.TopupDecidedPayload;
import ma.naja7i.payment.infrastructure.messaging.payload.TopupSubmittedPayload;
import ma.naja7i.payment.infrastructure.persistence.OutboxRepository;
import ma.naja7i.payment.infrastructure.persistence.TopupRepository;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopupService {

    public static final int MAX_PAGE_SIZE = 200;

    private final TopupRepository topupRepository;
    private final OutboxRepository outboxRepository;
    private final BalanceLedger balanceLedger;
    private final KafkaEventEnvelopeBuilder envelopeBuilder;

    // ============================================================ STUDENT

    /** Submit a new top-up request. Awaits admin approval; does NOT credit the wallet yet. */
    @Transactional
    public Topup submit(UUID studentId, TopupMethod method, long amountMinor, String proofUrl) {
        if (amountMinor <= 0L) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Amount must be positive", Map.of("amountMinor", amountMinor));
        }
        if (method == TopupMethod.BANK_TRANSFER && (proofUrl == null || proofUrl.isBlank())) {
            // Cash/WhatsApp may skip proof; bank transfer requires a screenshot.
            throw new BusinessException(ErrorCode.PAYMENT_TOPUP_PROOF_REQUIRED,
                    "Proof URL required for BANK_TRANSFER");
        }
        Topup t = topupRepository.saveAndFlush(Topup.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .method(method)
                .amountMinor(amountMinor)
                .currency("MAD")
                .proofUrl(proofUrl)
                .status(TopupStatus.EN_ATTENTE_ADMIN)
                .build());

        OutboxEvent row = envelopeBuilder.toOutbox(t.getId(),
                EventTopics.PAYMENT_TOPUP_SUBMITTED, studentId.toString(),
                new TopupSubmittedPayload(t.getId(), studentId,
                        toMajor(amountMinor), amountMinor, method.name(), proofUrl));
        outboxRepository.save(row);
        return t;
    }

    @Transactional(readOnly = true)
    public Page<Topup> listForStudent(UUID studentId, TopupStatus status, int page, int size) {
        return topupRepository.findForStudent(studentId, status,
                clamp(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    // ============================================================ ADMIN

    @Transactional(readOnly = true)
    public Page<Topup> listForAdmin(TopupStatus status, int page, int size) {
        return topupRepository.findForAdmin(status,
                clamp(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * Admin decision on a pending top-up.
     *
     * @param decision    'APPROVE' | 'MODIFY' | 'REJECT'
     * @param approvedMinor required for MODIFY (the new approved amount); ignored otherwise
     * @return the updated topup row
     */
    @Transactional
    public Topup decide(UUID topupId, UUID adminId, String decision,
                        Long approvedMinor, String notes, UUID traceId) {
        Topup t = topupRepository.findById(topupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Topup not found", Map.of("topupId", topupId)));
        if (t.getStatus() != TopupStatus.EN_ATTENTE_ADMIN) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "Topup already decided",
                    Map.of("topupId", topupId, "status", t.getStatus()));
        }

        long creditMinor = 0L;
        TopupStatus newStatus;
        switch (decision) {
            case "APPROVE" -> {
                newStatus = TopupStatus.APPROVED;
                creditMinor = t.getAmountMinor();
                t.setApprovedAmountMinor(t.getAmountMinor());
            }
            case "MODIFY" -> {
                if (approvedMinor == null || approvedMinor <= 0) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                            "MODIFY requires a positive approvedAmount");
                }
                newStatus = TopupStatus.MODIFIED;
                creditMinor = approvedMinor;
                t.setApprovedAmountMinor(approvedMinor);
            }
            case "REJECT" -> {
                newStatus = TopupStatus.REJECTED;
                t.setApprovedAmountMinor(0L);
            }
            default -> throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Unknown decision: " + decision);
        }
        t.setStatus(newStatus);
        t.setAdminNotes(notes);
        t.setDecidedBy(adminId);
        t.setDecidedAt(Instant.now());
        topupRepository.saveAndFlush(t);

        if (creditMinor > 0) {
            balanceLedger.apply(t.getStudentId(), creditMinor,
                    AuditOperation.TOPUP, AuditReferenceType.TOPUP, t.getId(),
                    adminId, "Topup " + decision + " by admin", traceId);
        }

        OutboxEvent row = envelopeBuilder.toOutbox(t.getId(),
                EventTopics.PAYMENT_TOPUP_DECIDED, t.getStudentId().toString(),
                new TopupDecidedPayload(t.getId(), t.getStudentId(), decision,
                        toMajor(creditMinor), creditMinor, adminId));
        outboxRepository.save(row);
        return t;
    }

    @Transactional(readOnly = true)
    public Topup get(UUID id) {
        return topupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Topup not found", Map.of("topupId", id)));
    }

    private static Pageable clamp(int page, int size, Sort sort) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize, sort);
    }

    private static BigDecimal toMajor(long minor) {
        return BigDecimal.valueOf(minor).movePointLeft(2);
    }
}

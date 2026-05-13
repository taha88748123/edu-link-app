package ma.naja7i.profile.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.profile.domain.ProfProfile;
import ma.naja7i.profile.infrastructure.messaging.KafkaPublisher;
import ma.naja7i.profile.infrastructure.messaging.payload.ProfRejectedPayload;
import ma.naja7i.profile.infrastructure.messaging.payload.ProfValidatedPayload;
import ma.naja7i.profile.infrastructure.persistence.ProfProfileRepository;
import ma.naja7i.shared.dto.ProfStatus;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminProfService {

    private final ProfProfileRepository profProfileRepository;
    private final KafkaPublisher kafkaPublisher;

    @Transactional(readOnly = true)
    public Page<ProfProfile> listPending(Pageable pageable) {
        return profProfileRepository.findByStatus(ProfStatus.EN_ATTENTE, pageable);
    }

    @Transactional
    public ProfProfile validate(UUID profId, UUID adminId, String adminNotes) {
        ProfProfile pp = loadOrThrow(profId);
        // For v1, allow validating from EN_ATTENTE or EN_ENTRETIEN_ADMIN. Reject from VALIDE.
        if (pp.getStatus() == ProfStatus.VALIDE) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "Prof already validated");
        }
        pp.setStatus(ProfStatus.VALIDE);
        pp.setValidatedAt(Instant.now());
        pp.setValidatedBy(adminId);
        if (adminNotes != null) pp.setAdminNotes(adminNotes);
        ProfProfile saved = profProfileRepository.save(pp);

        kafkaPublisher.publish(
                EventTopics.PROFILE_PROF_VALIDATED,
                profId.toString(),
                new ProfValidatedPayload(profId, adminId, saved.getValidatedAt()));
        return saved;
    }

    @Transactional
    public ProfProfile reject(UUID profId, UUID adminId, String reason) {
        ProfProfile pp = loadOrThrow(profId);
        if (pp.getStatus() == ProfStatus.VALIDE) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "Cannot reject an already validated prof — suspend instead");
        }
        pp.setStatus(ProfStatus.REJETE);
        pp.setAdminNotes(reason);
        ProfProfile saved = profProfileRepository.save(pp);

        kafkaPublisher.publish(
                EventTopics.PROFILE_PROF_REJECTED,
                profId.toString(),
                new ProfRejectedPayload(profId, reason));
        return saved;
    }

    @Transactional
    public ProfProfile suspend(UUID profId, UUID adminId, String reason) {
        ProfProfile pp = loadOrThrow(profId);
        if (pp.getStatus() == ProfStatus.SUSPENDU) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION, "Already suspended");
        }
        pp.setStatus(ProfStatus.SUSPENDU);
        if (reason != null) pp.setAdminNotes(reason);
        return profProfileRepository.save(pp);
    }

    private ProfProfile loadOrThrow(UUID profId) {
        return profProfileRepository.findById(profId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
    }
}

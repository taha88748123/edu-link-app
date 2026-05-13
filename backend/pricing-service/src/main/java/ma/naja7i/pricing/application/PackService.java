package ma.naja7i.pricing.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.pricing.domain.PricingPack;
import ma.naja7i.pricing.infrastructure.persistence.PricingPackRepository;
import ma.naja7i.pricing.infrastructure.web.dto.CreatePackRequest;
import ma.naja7i.pricing.infrastructure.web.dto.UpdatePackRequest;
import ma.naja7i.pricing.infrastructure.web.mapper.MoneyConverter;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** CRUD and ownership rules for {@link PricingPack} (multi-session bundles). */
@Service
@RequiredArgsConstructor
public class PackService {

    private final PricingPackRepository repo;

    @Transactional(readOnly = true)
    public List<PricingPack> listActiveByProf(UUID profId) {
        return repo.findByProfIdAndActiveTrue(profId);
    }

    @Transactional(readOnly = true)
    public List<PricingPack> searchActive(UUID profId, String subject, SchoolLevel level, Modality modality) {
        return repo.searchActive(profId, subject, level, modality);
    }

    @Transactional
    public PricingPack create(UUID profId, CreatePackRequest req) {
        long totalMinor = MoneyConverter.mad2Minor(req.totalAmountMad());
        PricingPack entity = PricingPack.builder()
                .profId(profId)
                .subjectCode(req.subjectCode())
                .level(req.level())
                .modality(req.modality())
                .sessionsCount(req.sessionsCount().shortValue())
                .discountPct(req.discountPct())
                .totalAmountMinor(totalMinor)
                .currency("MAD")
                .active(true)
                .build();
        return repo.save(entity);
    }

    @Transactional
    public PricingPack update(UUID profId, UUID packId, UpdatePackRequest req) {
        PricingPack entity = loadOwned(profId, packId);
        entity.setSubjectCode(req.subjectCode());
        entity.setLevel(req.level());
        entity.setModality(req.modality());
        entity.setSessionsCount(req.sessionsCount().shortValue());
        entity.setDiscountPct(req.discountPct());
        entity.setTotalAmountMinor(MoneyConverter.mad2Minor(req.totalAmountMad()));
        if (req.active() != null) entity.setActive(req.active());
        return repo.save(entity);
    }

    @Transactional
    public void softDelete(UUID profId, UUID packId) {
        PricingPack entity = loadOwned(profId, packId);
        entity.setActive(false);
        repo.save(entity);
    }

    private PricingPack loadOwned(UUID profId, UUID packId) {
        PricingPack entity = repo.findById(packId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Pack not found",
                        Map.of("packId", packId.toString())));
        if (!entity.getProfId().equals(profId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Pack not found",
                    Map.of("packId", packId.toString()));
        }
        return entity;
    }
}

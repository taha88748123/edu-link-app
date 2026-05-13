package ma.naja7i.pricing.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.pricing.domain.ProfPricing;
import ma.naja7i.pricing.infrastructure.persistence.ProfPricingRepository;
import ma.naja7i.pricing.infrastructure.web.dto.CreateRateRequest;
import ma.naja7i.pricing.infrastructure.web.dto.UpdateRateRequest;
import ma.naja7i.pricing.infrastructure.web.mapper.MoneyConverter;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** CRUD and ownership rules for {@link ProfPricing} (single-session rate rows). */
@Service
@RequiredArgsConstructor
public class PricingService {

    private final ProfPricingRepository repo;

    @Transactional(readOnly = true)
    public List<ProfPricing> listActiveByProf(UUID profId) {
        return repo.findByProfIdAndActiveTrue(profId);
    }

    @Transactional(readOnly = true)
    public List<ProfPricing> searchActive(UUID profId, String subject, SchoolLevel level, Modality modality) {
        return repo.searchActive(profId, subject, level, modality);
    }

    @Transactional
    public ProfPricing create(UUID profId, CreateRateRequest req) {
        long amountMinor = MoneyConverter.mad2Minor(req.amountMad());
        ProfPricing entity = ProfPricing.builder()
                .profId(profId)
                .subjectCode(req.subjectCode())
                .level(req.level())
                .modality(req.modality())
                .durationMinutes(req.durationMinutes().shortValue())
                .dayOfWeek(req.dayOfWeek() == null ? null : req.dayOfWeek().shortValue())
                .amountMinor(amountMinor)
                .currency("MAD")
                .active(true)
                .build();
        try {
            return repo.save(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Duplicate rate for this (subject, level, modality, duration, dayOfWeek)",
                    Map.of(
                            "subjectCode", req.subjectCode(),
                            "level", req.level().name(),
                            "modality", req.modality().name(),
                            "durationMinutes", req.durationMinutes(),
                            "dayOfWeek", String.valueOf(req.dayOfWeek())
                    ));
        }
    }

    @Transactional
    public ProfPricing update(UUID profId, UUID rateId, UpdateRateRequest req) {
        ProfPricing entity = loadOwned(profId, rateId);
        entity.setSubjectCode(req.subjectCode());
        entity.setLevel(req.level());
        entity.setModality(req.modality());
        entity.setDurationMinutes(req.durationMinutes().shortValue());
        entity.setDayOfWeek(req.dayOfWeek() == null ? null : req.dayOfWeek().shortValue());
        entity.setAmountMinor(MoneyConverter.mad2Minor(req.amountMad()));
        if (req.active() != null) entity.setActive(req.active());
        try {
            return repo.save(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Duplicate rate for this (subject, level, modality, duration, dayOfWeek)",
                    Map.of("rateId", rateId.toString()));
        }
    }

    @Transactional
    public void softDelete(UUID profId, UUID rateId) {
        ProfPricing entity = loadOwned(profId, rateId);
        entity.setActive(false);
        repo.save(entity);
    }

    /**
     * Loads a rate and verifies the requesting prof owns it.
     * Returns 404 to avoid leaking existence to a non-owner.
     */
    private ProfPricing loadOwned(UUID profId, UUID rateId) {
        ProfPricing entity = repo.findById(rateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Rate not found",
                        Map.of("rateId", rateId.toString())));
        if (!entity.getProfId().equals(profId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Rate not found",
                    Map.of("rateId", rateId.toString()));
        }
        return entity;
    }
}

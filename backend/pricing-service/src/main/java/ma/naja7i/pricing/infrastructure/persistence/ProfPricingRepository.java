package ma.naja7i.pricing.infrastructure.persistence;

import ma.naja7i.pricing.domain.ProfPricing;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfPricingRepository extends JpaRepository<ProfPricing, UUID> {

    List<ProfPricing> findByProfIdAndActiveTrue(UUID profId);

    @Query("""
            SELECT p FROM ProfPricing p
             WHERE p.profId = :profId
               AND p.active = true
               AND (:subject IS NULL OR p.subjectCode = :subject)
               AND (:level   IS NULL OR p.level      = :level)
               AND (:modality IS NULL OR p.modality  = :modality)
            """)
    List<ProfPricing> searchActive(@Param("profId") UUID profId,
                                   @Param("subject") String subject,
                                   @Param("level") SchoolLevel level,
                                   @Param("modality") Modality modality);

    /** Exact match including a specific day-of-week (used by the quote engine). */
    Optional<ProfPricing> findFirstByProfIdAndSubjectCodeAndLevelAndModalityAndDurationMinutesAndDayOfWeekAndActiveTrue(
            UUID profId, String subjectCode, SchoolLevel level, Modality modality,
            Short durationMinutes, Short dayOfWeek);

    /** Fallback match where {@code day_of_week IS NULL}. */
    Optional<ProfPricing> findFirstByProfIdAndSubjectCodeAndLevelAndModalityAndDurationMinutesAndDayOfWeekIsNullAndActiveTrue(
            UUID profId, String subjectCode, SchoolLevel level, Modality modality, Short durationMinutes);
}

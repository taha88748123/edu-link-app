package ma.naja7i.pricing.infrastructure.persistence;

import ma.naja7i.pricing.domain.PricingPack;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PricingPackRepository extends JpaRepository<PricingPack, UUID> {

    List<PricingPack> findByProfIdAndActiveTrue(UUID profId);

    @Query("""
            SELECT p FROM PricingPack p
             WHERE p.profId = :profId
               AND p.active = true
               AND (:subject IS NULL OR p.subjectCode = :subject)
               AND (:level   IS NULL OR p.level      = :level)
               AND (:modality IS NULL OR p.modality  = :modality)
            """)
    List<PricingPack> searchActive(@Param("profId") UUID profId,
                                   @Param("subject") String subject,
                                   @Param("level") SchoolLevel level,
                                   @Param("modality") Modality modality);
}

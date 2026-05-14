package ma.naja7i.ai.infrastructure.persistence;

import ma.naja7i.ai.domain.MatchingFeatures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * JSONB containment queries written as native SQL — Hibernate / JPA do not
 * have a portable operator for {@code features ->> 'subjects' ? :code}.
 */
public interface MatchingFeaturesRepository extends JpaRepository<MatchingFeatures, UUID> {

    /**
     * Find every PROF feature row whose JSONB {@code subjects}, {@code levels}
     * and {@code modalities} arrays contain the requested values. The Postgres
     * {@code ?} JSONB existence operator is escaped as {@code ??} for JPA.
     */
    @Query(value = """
            SELECT * FROM matching_features
            WHERE role = 'PROF'
              AND (features -> 'subjects')   ?? :subjectCode
              AND (features -> 'levels')     ?? :level
              AND (features -> 'modalities') ?? :modality
            """, nativeQuery = true)
    List<MatchingFeatures> findCandidateProfs(@Param("subjectCode") String subjectCode,
                                              @Param("level") String level,
                                              @Param("modality") String modality);
}

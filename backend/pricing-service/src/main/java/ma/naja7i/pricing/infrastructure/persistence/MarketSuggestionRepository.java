package ma.naja7i.pricing.infrastructure.persistence;

import ma.naja7i.pricing.domain.MarketSuggestion;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.SchoolLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketSuggestionRepository extends JpaRepository<MarketSuggestion, UUID> {

    /** Returns the most recently computed suggestion matching the query. */
    Optional<MarketSuggestion>
        findFirstByProfIdAndSubjectCodeAndLevelAndModalityAndDurationMinutesOrderByComputedAtDesc(
            UUID profId, String subjectCode, SchoolLevel level, Modality modality, Short durationMinutes);
}

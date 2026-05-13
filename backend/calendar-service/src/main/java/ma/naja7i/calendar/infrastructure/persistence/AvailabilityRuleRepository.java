package ma.naja7i.calendar.infrastructure.persistence;

import ma.naja7i.calendar.domain.AvailabilityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, UUID> {

    List<AvailabilityRule> findByProfIdOrderByCreatedAtDesc(UUID profId);

    List<AvailabilityRule> findByProfIdAndActiveTrue(UUID profId);

    List<AvailabilityRule> findByActiveTrue();
}

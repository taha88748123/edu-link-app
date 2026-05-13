package ma.naja7i.profile.infrastructure.persistence;

import ma.naja7i.profile.domain.ReviewStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewStatRepository extends JpaRepository<ReviewStat, UUID> {
}

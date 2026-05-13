package ma.naja7i.profile.infrastructure.persistence;

import ma.naja7i.profile.domain.Diploma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiplomaRepository extends JpaRepository<Diploma, UUID> {

    List<Diploma> findByUserId(UUID userId);

    long countByUserId(UUID userId);
}

package ma.naja7i.profile.infrastructure.persistence;

import ma.naja7i.profile.domain.ProfProfile;
import ma.naja7i.shared.dto.ProfStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProfProfileRepository extends JpaRepository<ProfProfile, UUID> {

    Page<ProfProfile> findByStatus(ProfStatus status, Pageable pageable);
}

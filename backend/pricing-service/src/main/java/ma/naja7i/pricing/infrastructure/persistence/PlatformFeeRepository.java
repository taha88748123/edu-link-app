package ma.naja7i.pricing.infrastructure.persistence;

import ma.naja7i.pricing.domain.PlatformFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformFeeRepository extends JpaRepository<PlatformFee, UUID> {

    Optional<PlatformFee> findFirstByNameAndActiveTrue(String name);
}

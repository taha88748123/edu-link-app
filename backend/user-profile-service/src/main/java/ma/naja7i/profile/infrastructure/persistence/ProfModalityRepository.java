package ma.naja7i.profile.infrastructure.persistence;

import ma.naja7i.profile.domain.ProfModalityEntity;
import ma.naja7i.profile.domain.ProfModalityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProfModalityRepository extends JpaRepository<ProfModalityEntity, ProfModalityId> {

    @Query("SELECT m FROM ProfModalityEntity m WHERE m.id.userId = :userId")
    List<ProfModalityEntity> findByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM ProfModalityEntity m WHERE m.id.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}

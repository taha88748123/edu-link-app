package ma.naja7i.profile.infrastructure.persistence;

import ma.naja7i.profile.domain.ProfLocation;
import ma.naja7i.profile.domain.ProfLocationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProfLocationRepository extends JpaRepository<ProfLocation, ProfLocationId> {

    @Query("SELECT l FROM ProfLocation l WHERE l.id.userId = :userId")
    List<ProfLocation> findByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM ProfLocation l WHERE l.id.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}

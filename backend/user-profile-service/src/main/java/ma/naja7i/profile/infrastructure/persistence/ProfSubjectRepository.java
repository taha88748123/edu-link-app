package ma.naja7i.profile.infrastructure.persistence;

import ma.naja7i.profile.domain.ProfSubject;
import ma.naja7i.profile.domain.ProfSubjectId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProfSubjectRepository extends JpaRepository<ProfSubject, ProfSubjectId> {

    @Query("SELECT s FROM ProfSubject s WHERE s.id.userId = :userId")
    List<ProfSubject> findByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM ProfSubject s WHERE s.id.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}

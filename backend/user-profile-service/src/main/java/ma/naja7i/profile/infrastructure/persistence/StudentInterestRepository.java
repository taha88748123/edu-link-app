package ma.naja7i.profile.infrastructure.persistence;

import ma.naja7i.profile.domain.StudentInterest;
import ma.naja7i.profile.domain.StudentInterestId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentInterestRepository extends JpaRepository<StudentInterest, StudentInterestId> {

    @Query("SELECT i FROM StudentInterest i WHERE i.id.userId = :userId")
    List<StudentInterest> findByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM StudentInterest i WHERE i.id.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}

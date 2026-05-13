package ma.naja7i.profile.infrastructure.persistence;

import ma.naja7i.profile.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByProfId(UUID profId, Pageable pageable);

    boolean existsByBookingIdAndStudentId(UUID bookingId, UUID studentId);
}

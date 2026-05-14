package ma.naja7i.video.infrastructure.persistence;

import ma.naja7i.video.domain.Session;
import ma.naja7i.video.domain.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);

    @Query("""
           SELECT s FROM Session s
           WHERE (s.studentId = :userId OR s.profId = :userId)
             AND (:status IS NULL OR s.status = :status)
             AND (:from IS NULL OR s.scheduledStart >= :from)
             AND (:to   IS NULL OR s.scheduledStart <= :to)
           """)
    Page<Session> findForParticipant(@Param("userId") UUID userId,
                                     @Param("status") SessionStatus status,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to,
                                     Pageable pageable);

    @Query("""
           SELECT s FROM Session s
           WHERE (:status IS NULL OR s.status = :status)
             AND (:from IS NULL OR s.scheduledStart >= :from)
             AND (:to   IS NULL OR s.scheduledStart <= :to)
           """)
    Page<Session> findForAdmin(@Param("status") SessionStatus status,
                               @Param("from") Instant from,
                               @Param("to") Instant to,
                               Pageable pageable);
}

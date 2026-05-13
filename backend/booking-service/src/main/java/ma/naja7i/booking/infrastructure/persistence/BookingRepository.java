package ma.naja7i.booking.infrastructure.persistence;

import ma.naja7i.booking.domain.Booking;
import ma.naja7i.shared.dto.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("""
           SELECT b FROM Booking b
           WHERE b.studentId = :studentId
             AND (:status IS NULL OR b.status = :status)
           """)
    Page<Booking> findForStudent(@Param("studentId") UUID studentId,
                                 @Param("status") BookingStatus status,
                                 Pageable pageable);

    @Query("""
           SELECT b FROM Booking b
           WHERE b.profId = :profId
             AND (:status IS NULL OR b.status = :status)
           """)
    Page<Booking> findForProf(@Param("profId") UUID profId,
                              @Param("status") BookingStatus status,
                              Pageable pageable);

    @Query("""
           SELECT b FROM Booking b
           WHERE (:status IS NULL OR b.status = :status)
             AND (:from IS NULL OR b.scheduledStart >= :from)
             AND (:to   IS NULL OR b.scheduledStart <= :to)
           """)
    Page<Booking> findForAdmin(@Param("status") BookingStatus status,
                               @Param("from") Instant from,
                               @Param("to") Instant to,
                               Pageable pageable);

    /** Used by the {@code auth.user.suspended} consumer to bulk-cancel a student's open bookings. */
    @Query("""
           SELECT b FROM Booking b
           WHERE b.studentId = :userId AND b.status IN :openStatuses
           """)
    List<Booking> findOpenForStudent(@Param("userId") UUID userId,
                                     @Param("openStatuses") List<BookingStatus> openStatuses);
}

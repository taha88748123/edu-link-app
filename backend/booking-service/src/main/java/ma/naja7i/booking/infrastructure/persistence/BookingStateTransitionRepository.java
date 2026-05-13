package ma.naja7i.booking.infrastructure.persistence;

import ma.naja7i.booking.domain.BookingStateTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingStateTransitionRepository extends JpaRepository<BookingStateTransition, Long> {

    List<BookingStateTransition> findByBookingIdOrderByCreatedAtAsc(UUID bookingId);
}

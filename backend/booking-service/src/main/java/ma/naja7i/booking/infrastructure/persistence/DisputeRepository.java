package ma.naja7i.booking.infrastructure.persistence;

import ma.naja7i.booking.domain.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    Optional<Dispute> findByBookingId(UUID bookingId);
}

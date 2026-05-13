package ma.naja7i.payment.infrastructure.persistence;

import ma.naja7i.payment.domain.Escrow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EscrowRepository extends JpaRepository<Escrow, UUID> {

    Optional<Escrow> findByBookingId(UUID bookingId);

    boolean existsByBookingId(UUID bookingId);
}

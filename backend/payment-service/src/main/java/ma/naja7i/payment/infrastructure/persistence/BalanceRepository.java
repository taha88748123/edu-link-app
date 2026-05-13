package ma.naja7i.payment.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import ma.naja7i.payment.domain.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BalanceRepository extends JpaRepository<Balance, UUID> {

    /**
     * Loads the balance row with a {@code SELECT ... FOR UPDATE} lock so that
     * concurrent balance mutations on the same user are serialised. Used by
     * {@link ma.naja7i.payment.application.BalanceLedger} on every balance change.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Balance b WHERE b.userId = :userId")
    Optional<Balance> findByIdForUpdate(@Param("userId") UUID userId);
}

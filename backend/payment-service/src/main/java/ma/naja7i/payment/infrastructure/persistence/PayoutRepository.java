package ma.naja7i.payment.infrastructure.persistence;

import ma.naja7i.payment.domain.Payout;
import ma.naja7i.payment.domain.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    @Query("""
           SELECT p FROM Payout p
           WHERE p.profId = :profId
             AND (:status IS NULL OR p.status = :status)
           """)
    Page<Payout> findForProf(@Param("profId") UUID profId,
                             @Param("status") PayoutStatus status,
                             Pageable pageable);

    @Query("""
           SELECT p FROM Payout p
           WHERE (:status IS NULL OR p.status = :status)
           """)
    Page<Payout> findForAdmin(@Param("status") PayoutStatus status, Pageable pageable);
}

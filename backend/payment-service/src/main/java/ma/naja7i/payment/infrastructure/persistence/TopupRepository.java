package ma.naja7i.payment.infrastructure.persistence;

import ma.naja7i.payment.domain.Topup;
import ma.naja7i.payment.domain.TopupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TopupRepository extends JpaRepository<Topup, UUID> {

    @Query("""
           SELECT t FROM Topup t
           WHERE t.studentId = :studentId
             AND (:status IS NULL OR t.status = :status)
           """)
    Page<Topup> findForStudent(@Param("studentId") UUID studentId,
                               @Param("status") TopupStatus status,
                               Pageable pageable);

    @Query("""
           SELECT t FROM Topup t
           WHERE (:status IS NULL OR t.status = :status)
           """)
    Page<Topup> findForAdmin(@Param("status") TopupStatus status, Pageable pageable);
}

package ma.naja7i.auth.infrastructure.persistence;

import ma.naja7i.auth.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}

package ma.naja7i.video.infrastructure.persistence;

import ma.naja7i.video.domain.SessionArtifact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionArtifactRepository extends JpaRepository<SessionArtifact, UUID> {

    List<SessionArtifact> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}

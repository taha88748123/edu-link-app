package ma.naja7i.video.infrastructure.web.dto;

import ma.naja7i.video.domain.ArtifactType;
import ma.naja7i.video.domain.SessionArtifact;

import java.time.Instant;
import java.util.UUID;

public record ArtifactView(
        UUID id,
        UUID sessionId,
        ArtifactType type,
        String content,
        UUID createdBy,
        Instant createdAt
) {
    public static ArtifactView from(SessionArtifact a) {
        return new ArtifactView(a.getId(), a.getSessionId(), a.getType(),
                a.getContent(), a.getCreatedBy(), a.getCreatedAt());
    }
}

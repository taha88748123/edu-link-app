package ma.naja7i.video.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.video.domain.ArtifactType;
import ma.naja7i.video.domain.Session;
import ma.naja7i.video.domain.SessionArtifact;
import ma.naja7i.video.infrastructure.persistence.SessionArtifactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Session-scoped artifact operations: snapshots of a whiteboard, quiz, code
 * block or note created during a video call. The caller MUST be a participant
 * (or ADMIN) of the parent session — enforced here, not in the controller.
 */
@Service
@RequiredArgsConstructor
public class ArtifactService {

    private final SessionService sessionService;
    private final SessionArtifactRepository artifactRepository;

    /**
     * Persist a new artifact. Throws if the caller is not a participant
     * (or ADMIN). The {@code content} is stored verbatim — schema is owned
     * by the producer.
     */
    @Transactional
    public SessionArtifact create(UUID sessionId, UUID callerId, Role callerRole,
                                  ArtifactType type, String content) {
        Session s = requireParticipantOrAdmin(sessionId, callerId, callerRole);
        SessionArtifact a = SessionArtifact.builder()
                .id(UUID.randomUUID())
                .sessionId(s.getId())
                .type(type)
                .content(content)
                .createdBy(callerId)
                .build();
        return artifactRepository.saveAndFlush(a);
    }

    /** List the artifacts for a session in chronological order. Same access rules as create. */
    @Transactional(readOnly = true)
    public List<SessionArtifact> list(UUID sessionId, UUID callerId, Role callerRole) {
        requireParticipantOrAdmin(sessionId, callerId, callerRole);
        return artifactRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    private Session requireParticipantOrAdmin(UUID sessionId, UUID callerId, Role callerRole) {
        Session s = sessionService.get(sessionId);
        boolean isParticipant = callerId != null
                && (callerId.equals(s.getProfId()) || callerId.equals(s.getStudentId()));
        if (!isParticipant && callerRole != Role.ADMIN) {
            // Mask existence — match SessionService pattern.
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Session not found", Map.of("sessionId", sessionId));
        }
        return s;
    }
}

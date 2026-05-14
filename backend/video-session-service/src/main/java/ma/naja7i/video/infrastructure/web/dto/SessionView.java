package ma.naja7i.video.infrastructure.web.dto;

import ma.naja7i.video.domain.Consent;
import ma.naja7i.video.domain.Session;
import ma.naja7i.video.domain.SessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-side view of a {@link Session}. The LiveKit room name is exposed so the
 * frontend can pre-fetch a token; recording_url is only included when present
 * (post-session, both consents granted).
 */
public record SessionView(
        UUID id,
        UUID bookingId,
        UUID profId,
        UUID studentId,
        String livekitRoom,
        Instant scheduledStart,
        Instant scheduledEnd,
        Instant actualStart,
        Instant actualEnd,
        Integer durationEffectiveSeconds,
        Boolean recordingEnabled,
        String recordingUrl,
        Instant recordingExpiresAt,
        List<Consent> consentGivenBy,
        SessionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static SessionView from(Session s) {
        if (s == null) return null;
        return new SessionView(
                s.getId(),
                s.getBookingId(),
                s.getProfId(),
                s.getStudentId(),
                s.getLivekitRoom(),
                s.getScheduledStart(),
                s.getScheduledEnd(),
                s.getActualStart(),
                s.getActualEnd(),
                s.getDurationEffectiveSeconds(),
                s.getRecordingEnabled(),
                s.getRecordingUrl(),
                s.getRecordingExpiresAt(),
                s.getConsentGivenBy(),
                s.getStatus(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}

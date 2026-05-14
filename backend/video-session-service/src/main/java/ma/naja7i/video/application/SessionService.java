package ma.naja7i.video.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import ma.naja7i.video.config.VideoProperties;
import ma.naja7i.video.domain.Consent;
import ma.naja7i.video.domain.ConsentScope;
import ma.naja7i.video.domain.OutboxEvent;
import ma.naja7i.video.domain.Session;
import ma.naja7i.video.domain.SessionStatus;
import ma.naja7i.video.infrastructure.messaging.payload.RecordingReadyPayload;
import ma.naja7i.video.infrastructure.messaging.payload.SessionEndedPayload;
import ma.naja7i.video.infrastructure.messaging.payload.SessionStartedPayload;
import ma.naja7i.video.infrastructure.persistence.OutboxRepository;
import ma.naja7i.video.infrastructure.persistence.SessionRepository;
import ma.naja7i.video.livekit.LiveKitTokenIssuer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * State machine + write operations for {@link Session}.
 *
 * <pre>
 *   (consumer)        booking.accepted  -> SCHEDULED
 *   (participant)     :start            -> LIVE   (SCHEDULED only)
 *   (participant)     :end              -> ENDED  (LIVE only)
 *   (admin / consumer):expire           -> EXPIRED (SCHEDULED only)
 * </pre>
 *
 * <p>Token issuance goes through {@link LiveKitTokenIssuer} (jwt / logging driver).
 * Every transition writes an outbox row in the same TX; {@link OutboxPoller}
 * publishes it.
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    public static final int MAX_PAGE_SIZE = 200;
    private static final Set<SessionStatus> JOINABLE = EnumSet.of(SessionStatus.SCHEDULED, SessionStatus.LIVE);
    private static final SecureRandom RNG = new SecureRandom();
    private static final String ROOM_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final SessionRepository sessionRepository;
    private final OutboxRepository outboxRepository;
    private final KafkaEventEnvelopeBuilder envelopeBuilder;
    private final LiveKitTokenIssuer tokenIssuer;
    private final VideoProperties properties;

    // ================================================================ queries

    @Transactional(readOnly = true)
    public Session get(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Session not found", Map.of("sessionId", sessionId)));
    }

    @Transactional(readOnly = true)
    public Session getOwned(UUID sessionId, UUID viewerId, Role viewerRole) {
        Session s = get(sessionId);
        if (viewerRole == Role.ADMIN) return s;
        if (isParticipant(s, viewerId)) return s;
        // Mask existence — same pattern as booking-service.
        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                "Session not found", Map.of("sessionId", sessionId));
    }

    @Transactional(readOnly = true)
    public Page<Session> listForCaller(UUID viewerId, Role viewerRole, SessionStatus status,
                                       Instant from, Instant to, int page, int size) {
        Pageable pageable = clamp(page, size, Sort.by(Sort.Direction.DESC, "scheduledStart"));
        if (viewerRole == Role.ADMIN) {
            return sessionRepository.findForAdmin(status, from, to, pageable);
        }
        if (viewerRole != Role.STUDENT && viewerRole != Role.PROF) {
            return Page.empty(pageable);
        }
        return sessionRepository.findForParticipant(viewerId, status, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Session> listForAdmin(SessionStatus status, Instant from, Instant to,
                                      int page, int size) {
        Pageable pageable = clamp(page, size, Sort.by(Sort.Direction.DESC, "scheduledStart"));
        return sessionRepository.findForAdmin(status, from, to, pageable);
    }

    // ================================================================ token

    /**
     * Issue a LiveKit JWT for {@code caller} to join {@code sessionId}. Throws
     * {@link BusinessException} if the caller is not a participant, the session
     * is not in a joinable state or outside the join window.
     */
    @Transactional(readOnly = true)
    public LiveKitTokenIssuer.Token issueToken(UUID sessionId, UUID callerId, Role callerRole) {
        Session s = get(sessionId);
        if (!isParticipant(s, callerId) && callerRole != Role.ADMIN) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE,
                    "Not a participant of this session",
                    Map.of("sessionId", sessionId));
        }
        if (!JOINABLE.contains(s.getStatus())) {
            throw new BusinessException(ErrorCode.VIDEO_SESSION_NOT_LIVE,
                    "Session not joinable",
                    Map.of("sessionId", sessionId, "status", s.getStatus()));
        }
        Instant now = Instant.now();
        VideoProperties.LiveKit lk = properties.getLivekit();
        Instant earliest = s.getScheduledStart().minus(Duration.ofMinutes(lk.getPreliveBufferMinutes()));
        Instant latest   = s.getScheduledEnd().plus(Duration.ofMinutes(lk.getTokenTtlBufferMinutes()));
        if (now.isBefore(earliest) || now.isAfter(latest)) {
            throw new BusinessException(ErrorCode.VIDEO_SESSION_NOT_LIVE,
                    "Outside join window",
                    Map.of("sessionId", sessionId,
                            "earliest", earliest, "latest", latest));
        }
        String roleLabel = roleLabel(s, callerId, callerRole);
        return tokenIssuer.mint(callerId, s.getLivekitRoom(), roleLabel,
                true, true, latest);
    }

    // ================================================================ transitions

    @Transactional
    public Session start(UUID sessionId, UUID callerId, Role callerRole) {
        Session s = get(sessionId);
        if (!isParticipant(s, callerId) && callerRole != Role.ADMIN) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE,
                    "Not a participant of this session",
                    Map.of("sessionId", sessionId));
        }
        if (s.getStatus() == SessionStatus.LIVE) {
            // Idempotent: a second :start by the other participant is a no-op.
            return s;
        }
        if (s.getStatus() != SessionStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.VIDEO_SESSION_NOT_LIVE,
                    "Cannot start from status " + s.getStatus(),
                    Map.of("sessionId", sessionId, "status", s.getStatus()));
        }
        Instant now = Instant.now();
        s.setStatus(SessionStatus.LIVE);
        s.setActualStart(now);
        Session saved = sessionRepository.saveAndFlush(s);
        enqueueOutbox(saved.getId(), EventTopics.VIDEO_SESSION_STARTED,
                saved.getBookingId().toString(),
                new SessionStartedPayload(saved.getId(), saved.getBookingId(), now));
        return saved;
    }

    @Transactional
    public Session end(UUID sessionId, UUID callerId, Role callerRole) {
        Session s = get(sessionId);
        if (!isParticipant(s, callerId) && callerRole != Role.ADMIN) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE,
                    "Not a participant of this session",
                    Map.of("sessionId", sessionId));
        }
        if (s.getStatus() == SessionStatus.ENDED) {
            return s;
        }
        if (s.getStatus() != SessionStatus.LIVE) {
            throw new BusinessException(ErrorCode.VIDEO_SESSION_NOT_LIVE,
                    "Cannot end from status " + s.getStatus(),
                    Map.of("sessionId", sessionId, "status", s.getStatus()));
        }
        Instant now = Instant.now();
        s.setStatus(SessionStatus.ENDED);
        s.setActualEnd(now);
        long durationSec = s.getActualStart() == null
                ? 0L
                : Math.max(0L, Duration.between(s.getActualStart(), now).getSeconds());
        s.setDurationEffectiveSeconds((int) Math.min(Integer.MAX_VALUE, durationSec));
        Session saved = sessionRepository.saveAndFlush(s);

        enqueueOutbox(saved.getId(), EventTopics.VIDEO_SESSION_ENDED,
                saved.getBookingId().toString(),
                new SessionEndedPayload(saved.getId(), saved.getBookingId(),
                        durationSec, saved.getRecordingUrl()));

        // v1 simulation: if recording was enabled by mutual consent, emit a
        // recording.ready event immediately (no real S3/MinIO post-processing).
        if (Boolean.TRUE.equals(saved.getRecordingEnabled())) {
            Instant expires = now.plus(Duration.ofDays(30));
            saved.setRecordingExpiresAt(expires);
            if (saved.getRecordingUrl() == null) {
                saved.setRecordingUrl("naja7i://recordings/" + saved.getId() + ".mp4");
            }
            sessionRepository.saveAndFlush(saved);
            enqueueOutbox(saved.getId(), "naja7i.video.recording.ready.v1",
                    saved.getBookingId().toString(),
                    new RecordingReadyPayload(saved.getId(), saved.getBookingId(),
                            saved.getRecordingUrl(), expires));
        }
        return saved;
    }

    /** Admin-only manual expire of a SCHEDULED session. */
    @Transactional
    public Session adminExpire(UUID sessionId) {
        Session s = get(sessionId);
        if (s.getStatus() != SessionStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.VIDEO_SESSION_NOT_LIVE,
                    "Only SCHEDULED sessions can be expired",
                    Map.of("sessionId", sessionId, "status", s.getStatus()));
        }
        s.setStatus(SessionStatus.EXPIRED);
        return sessionRepository.saveAndFlush(s);
    }

    // ================================================================ consent

    /**
     * Appends a consent entry if not already present for the (userId, scope) pair.
     * When both participants have granted RECORDING, flips {@code recordingEnabled}
     * on the row.
     */
    @Transactional
    public Session grantConsent(UUID sessionId, UUID callerId, Role callerRole, ConsentScope scope) {
        Session s = get(sessionId);
        if (!isParticipant(s, callerId) && callerRole != Role.ADMIN) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE,
                    "Not a participant of this session",
                    Map.of("sessionId", sessionId));
        }
        if (s.getStatus() == SessionStatus.ENDED
                || s.getStatus() == SessionStatus.EXPIRED
                || s.getStatus() == SessionStatus.FAILED) {
            throw new BusinessException(ErrorCode.VIDEO_SESSION_NOT_LIVE,
                    "Cannot consent on terminal status " + s.getStatus(),
                    Map.of("sessionId", sessionId));
        }
        List<Consent> list = s.getConsentGivenBy() == null
                ? new ArrayList<>()
                : new ArrayList<>(s.getConsentGivenBy());
        boolean exists = list.stream().anyMatch(
                c -> callerId.equals(c.userId()) && scope == c.scope());
        if (!exists) {
            list.add(new Consent(callerId, Instant.now(), scope));
            s.setConsentGivenBy(list);
        }
        if (scope == ConsentScope.RECORDING && bothConsentedRecording(s, list)) {
            s.setRecordingEnabled(Boolean.TRUE);
        }
        return sessionRepository.saveAndFlush(s);
    }

    // ================================================================ consumed: booking.accepted

    /**
     * Idempotently create a SCHEDULED session for a freshly-accepted booking.
     * Re-running on a booking we already know is a no-op.
     */
    @Transactional
    public Session onBookingAccepted(UUID bookingId, UUID studentId, UUID profId,
                                     Instant scheduledStart, Instant scheduledEnd) {
        if (sessionRepository.existsByBookingId(bookingId)) {
            return sessionRepository.findByBookingId(bookingId).orElseThrow();
        }
        Instant now = Instant.now();
        Instant start = scheduledStart != null ? scheduledStart : now.plus(Duration.ofMinutes(5));
        Instant end = scheduledEnd != null
                ? scheduledEnd
                : start.plus(Duration.ofMinutes(properties.getDefaultDurationMinutes()));
        Session session = Session.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .studentId(studentId == null ? unknownPlaceholder("STUDENT", bookingId) : studentId)
                .profId(profId == null ? unknownPlaceholder("PROF", bookingId) : profId)
                .livekitRoom(generateRoomName(bookingId))
                .scheduledStart(start)
                .scheduledEnd(end)
                .recordingEnabled(Boolean.FALSE)
                .consentGivenBy(new ArrayList<>())
                .status(SessionStatus.SCHEDULED)
                .build();
        return sessionRepository.saveAndFlush(session);
    }

    /**
     * Idempotently mark a SCHEDULED session as EXPIRED when its booking is
     * cancelled or declined. No-op on terminal / live sessions.
     */
    @Transactional
    public void onBookingTerminated(UUID bookingId) {
        sessionRepository.findByBookingId(bookingId).ifPresent(s -> {
            if (s.getStatus() == SessionStatus.SCHEDULED) {
                s.setStatus(SessionStatus.EXPIRED);
                sessionRepository.saveAndFlush(s);
            } else {
                log.debug("booking termination on non-SCHEDULED session: sessionId={} status={}",
                        s.getId(), s.getStatus());
            }
        });
    }

    // ================================================================ internals

    private boolean isParticipant(Session s, UUID userId) {
        return userId != null
                && (userId.equals(s.getProfId()) || userId.equals(s.getStudentId()));
    }

    private String roleLabel(Session s, UUID callerId, Role callerRole) {
        if (callerRole == Role.ADMIN) return "ADMIN";
        if (callerId.equals(s.getProfId())) return "PROF";
        if (callerId.equals(s.getStudentId())) return "STUDENT";
        return callerRole == null ? "GUEST" : callerRole.name();
    }

    private boolean bothConsentedRecording(Session s, List<Consent> list) {
        boolean prof = list.stream().anyMatch(
                c -> c.scope() == ConsentScope.RECORDING && s.getProfId().equals(c.userId()));
        boolean stu  = list.stream().anyMatch(
                c -> c.scope() == ConsentScope.RECORDING && s.getStudentId().equals(c.userId()));
        return prof && stu;
    }

    private void enqueueOutbox(UUID aggregateId, String eventType,
                               String partitionKey, Object payload) {
        OutboxEvent row = envelopeBuilder.toOutbox(
                aggregateId, eventType, partitionKey, payload);
        outboxRepository.save(row);
    }

    static String generateRoomName(UUID bookingId) {
        String prefix = bookingId.toString().replace("-", "").substring(0, 8);
        StringBuilder suffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            suffix.append(ROOM_ALPHABET.charAt(RNG.nextInt(ROOM_ALPHABET.length())));
        }
        return "naja7i-" + prefix + "-" + suffix;
    }

    /**
     * Defensive: if the producer omitted studentId/profId we generate a deterministic
     * placeholder UUID so the DB CHECK / NOT NULL stays satisfied. This lets us land
     * the row and an operator can later reconcile from booking-service. NEVER reused
     * as an actual user identity.
     */
    private static UUID unknownPlaceholder(String marker, UUID bookingId) {
        return UUID.nameUUIDFromBytes(("naja7i-unknown-" + marker + "-" + bookingId).getBytes());
    }

    private Pageable clamp(int page, int size, Sort sort) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize, sort);
    }
}

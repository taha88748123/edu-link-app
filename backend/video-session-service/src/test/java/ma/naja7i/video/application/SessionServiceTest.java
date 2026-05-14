package ma.naja7i.video.application;

import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.video.config.VideoProperties;
import ma.naja7i.video.domain.OutboxEvent;
import ma.naja7i.video.domain.Session;
import ma.naja7i.video.domain.SessionStatus;
import ma.naja7i.video.infrastructure.persistence.OutboxRepository;
import ma.naja7i.video.infrastructure.persistence.SessionRepository;
import ma.naja7i.video.livekit.LiveKitTokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SessionService}'s state machine. No DB / Kafka — we
 * mock repositories and the envelope builder so each test asserts pure logic.
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock SessionRepository sessionRepository;
    @Mock OutboxRepository outboxRepository;
    @Mock KafkaEventEnvelopeBuilder envelopeBuilder;
    @Mock LiveKitTokenIssuer tokenIssuer;

    SessionService service;
    VideoProperties properties;

    UUID sessionId;
    UUID profId;
    UUID studentId;
    UUID bookingId;

    @BeforeEach
    void setup() {
        properties = new VideoProperties();
        service = new SessionService(sessionRepository, outboxRepository,
                envelopeBuilder, tokenIssuer, properties);
        sessionId = UUID.randomUUID();
        profId    = UUID.randomUUID();
        studentId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
    }

    private Session scheduledSession() {
        Instant start = Instant.now().minus(Duration.ofMinutes(1));
        return Session.builder()
                .id(sessionId)
                .bookingId(bookingId)
                .profId(profId)
                .studentId(studentId)
                .livekitRoom("naja7i-test-room")
                .scheduledStart(start)
                .scheduledEnd(start.plus(Duration.ofMinutes(60)))
                .status(SessionStatus.SCHEDULED)
                .recordingEnabled(Boolean.FALSE)
                .version(0)
                .build();
    }

    private Session liveSession() {
        Session s = scheduledSession();
        s.setStatus(SessionStatus.LIVE);
        s.setActualStart(Instant.now().minus(Duration.ofMinutes(5)));
        return s;
    }

    // ============================================================ :start

    @Test
    void start_transitionsScheduledToLive_andEnqueuesOutbox() {
        Session s = scheduledSession();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(s));
        when(sessionRepository.saveAndFlush(any(Session.class))).thenAnswer(i -> i.getArgument(0));
        when(envelopeBuilder.toOutbox(any(UUID.class), any(String.class), any(String.class), any()))
                .thenReturn(OutboxEvent.builder().id(UUID.randomUUID()).build());

        Session result = service.start(sessionId, studentId, Role.STUDENT);

        assertThat(result.getStatus()).isEqualTo(SessionStatus.LIVE);
        assertThat(result.getActualStart()).isNotNull();
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    void start_idempotent_whenAlreadyLive() {
        Session s = liveSession();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(s));

        Session result = service.start(sessionId, profId, Role.PROF);

        assertThat(result.getStatus()).isEqualTo(SessionStatus.LIVE);
        verify(sessionRepository, never()).saveAndFlush(any(Session.class));
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void start_rejected_whenCallerIsNotParticipant() {
        Session s = scheduledSession();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.start(sessionId, UUID.randomUUID(), Role.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.AUTH_FORBIDDEN_ROLE);
    }

    @Test
    void start_rejected_fromTerminalStatus() {
        Session s = scheduledSession();
        s.setStatus(SessionStatus.ENDED);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.start(sessionId, studentId, Role.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VIDEO_SESSION_NOT_LIVE);
    }

    // ============================================================ :end

    @Test
    void end_transitionsLiveToEnded_andComputesDuration() {
        Session s = liveSession();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(s));
        when(sessionRepository.saveAndFlush(any(Session.class))).thenAnswer(i -> i.getArgument(0));
        when(envelopeBuilder.toOutbox(any(UUID.class), any(String.class), any(String.class), any()))
                .thenReturn(OutboxEvent.builder().id(UUID.randomUUID()).build());

        Session result = service.end(sessionId, profId, Role.PROF);

        assertThat(result.getStatus()).isEqualTo(SessionStatus.ENDED);
        assertThat(result.getActualEnd()).isNotNull();
        assertThat(result.getDurationEffectiveSeconds()).isNotNull();
        assertThat(result.getDurationEffectiveSeconds()).isGreaterThanOrEqualTo(0);
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

    @Test
    void end_idempotent_whenAlreadyEnded() {
        Session s = liveSession();
        s.setStatus(SessionStatus.ENDED);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(s));

        Session result = service.end(sessionId, studentId, Role.STUDENT);

        assertThat(result.getStatus()).isEqualTo(SessionStatus.ENDED);
        verify(sessionRepository, never()).saveAndFlush(any(Session.class));
    }

    @Test
    void end_rejected_whenNotLive() {
        Session s = scheduledSession();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.end(sessionId, profId, Role.PROF))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VIDEO_SESSION_NOT_LIVE);
    }

    // ============================================================ issueToken (role / window)

    @Test
    void issueToken_rejected_outsideJoinWindow() {
        // Session scheduled far in the future — outside the prelive buffer (5min).
        Session s = scheduledSession();
        Instant farFuture = Instant.now().plus(Duration.ofHours(2));
        s.setScheduledStart(farFuture);
        s.setScheduledEnd(farFuture.plus(Duration.ofMinutes(60)));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.issueToken(sessionId, studentId, Role.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.VIDEO_SESSION_NOT_LIVE);
    }

    @Test
    void issueToken_rejected_whenCallerIsNotParticipant() {
        Session s = scheduledSession();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.issueToken(sessionId, UUID.randomUUID(), Role.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.AUTH_FORBIDDEN_ROLE);
    }

    @Test
    void issueToken_succeeds_insideJoinWindow_forParticipant() {
        Session s = scheduledSession();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(s));
        LiveKitTokenIssuer.Token stub = new LiveKitTokenIssuer.Token(
                "stub", "wss://x", s.getLivekitRoom(), Instant.now().plusSeconds(3600));
        when(tokenIssuer.mint(eq(studentId), eq(s.getLivekitRoom()), any(String.class),
                eq(true), eq(true), any(Instant.class))).thenReturn(stub);

        LiveKitTokenIssuer.Token result = service.issueToken(sessionId, studentId, Role.STUDENT);

        assertThat(result).isEqualTo(stub);
    }
}

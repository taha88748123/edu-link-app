package ma.naja7i.video.application;

import ma.naja7i.shared.events.DomainEvent;
import ma.naja7i.video.config.VideoProperties;
import ma.naja7i.video.domain.OutboxEvent;
import ma.naja7i.video.infrastructure.persistence.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock OutboxRepository outboxRepository;
    @Mock KafkaTemplate<String, DomainEvent<?>> kafkaTemplate;
    @Mock KafkaEventEnvelopeBuilder envelopeBuilder;

    OutboxPoller poller;
    VideoProperties properties;

    @BeforeEach
    void setup() {
        properties = new VideoProperties();
        poller = new OutboxPoller(outboxRepository, kafkaTemplate, envelopeBuilder, properties);
    }

    private OutboxEvent pending(String type) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.randomUUID())
                .eventType(type)
                .partitionKey(UUID.randomUUID().toString())
                .payload("{}")
                .createdAt(Instant.now())
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CompletableFuture<SendResult<String, DomainEvent<?>>> completed() {
        CompletableFuture cf = CompletableFuture.completedFuture(null);
        return cf;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CompletableFuture<SendResult<String, DomainEvent<?>>> failed(Throwable t) {
        CompletableFuture cf = new CompletableFuture<>();
        cf.completeExceptionally(t);
        return cf;
    }

    @Test
    void publishPending_noRows_doesNothing() {
        when(outboxRepository.fetchPendingForPublishing(any(Pageable.class)))
                .thenReturn(List.of());

        poller.publishPending();

        verify(kafkaTemplate, never()).send(any(String.class), any(String.class), any());
        verify(outboxRepository, never()).markPublished(any(), any());
    }

    @Test
    void publishPending_publishesAndMarks() {
        OutboxEvent row = pending("naja7i.video.session.started.v1");
        when(outboxRepository.fetchPendingForPublishing(any(Pageable.class)))
                .thenReturn(List.of(row));
        DomainEvent<Object> envelope = DomainEvent.create(
                "naja7i.video.session.started.v1", "video-session-service", new Object());
        when(envelopeBuilder.toDomainEvent(eq(row))).thenReturn(envelope);
        when(kafkaTemplate.send(eq("naja7i.video.session.started.v1"),
                eq(row.getPartitionKey()), any()))
                .thenReturn(completed());

        poller.publishPending();

        verify(kafkaTemplate).send(eq("naja7i.video.session.started.v1"),
                eq(row.getPartitionKey()), any());
        verify(outboxRepository).markPublished(eq(row.getId()), any(Instant.class));
    }

    @Test
    void publishPending_sendFailure_leavesRowForRetry() {
        OutboxEvent row = pending("naja7i.video.session.ended.v1");
        when(outboxRepository.fetchPendingForPublishing(any(Pageable.class)))
                .thenReturn(List.of(row));
        DomainEvent<Object> envelope = DomainEvent.create(
                "naja7i.video.session.ended.v1", "video-session-service", new Object());
        when(envelopeBuilder.toDomainEvent(eq(row))).thenReturn(envelope);
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(failed(new RuntimeException("kafka down")));

        poller.publishPending();

        verify(outboxRepository, never()).markPublished(any(), any());
    }
}

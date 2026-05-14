package ma.naja7i.ai.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.ai.config.AiProperties;
import ma.naja7i.ai.domain.OutboxEvent;
import ma.naja7i.ai.infrastructure.persistence.OutboxRepository;
import ma.naja7i.shared.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Publishes pending outbox rows to Kafka on a fixed cadence. One TX per
 * tick; pessimistic locking lets multiple replicas share the work.
 * Per-row failures stay un-published and get retried on the next tick.
 */
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, DomainEvent<?>> kafkaTemplate;
    private final KafkaEventEnvelopeBuilder envelopeBuilder;
    private final AiProperties properties;

    @Scheduled(fixedDelayString = "${naja7i.ai.outbox-poll-ms:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = outboxRepository.fetchPendingForPublishing(
                PageRequest.of(0, properties.getOutboxBatchSize()));
        if (batch.isEmpty()) return;

        int sent = 0;
        for (OutboxEvent row : batch) {
            try {
                DomainEvent<Object> envelope = envelopeBuilder.toDomainEvent(row);
                kafkaTemplate.send(row.getEventType(), row.getPartitionKey(), envelope).get();
                outboxRepository.markPublished(row.getId(), Instant.now());
                sent++;
            } catch (Exception ex) {
                log.warn("Outbox publish failed (will retry): id={} eventType={} cause={}",
                        row.getId(), row.getEventType(), ex.getMessage());
            }
        }
        if (sent > 0) {
            log.info("Outbox published {}/{} event(s)", sent, batch.size());
        }
    }
}

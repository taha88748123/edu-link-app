package ma.naja7i.booking.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.booking.config.BookingProperties;
import ma.naja7i.booking.domain.OutboxEvent;
import ma.naja7i.booking.infrastructure.persistence.OutboxRepository;
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
 * Publishes pending outbox rows to Kafka on a scheduled cadence. One TX per tick:
 * <ol>
 *   <li>Fetch up to {@code outboxBatchSize} rows where {@code published_at IS NULL},
 *       locking them with {@code FOR UPDATE SKIP LOCKED} so concurrent pollers do not
 *       pick the same rows.</li>
 *   <li>Send each row to Kafka (synchronous get) with key=partitionKey,
 *       value=DomainEvent envelope.</li>
 *   <li>Stamp {@code published_at = NOW()} for successful publishes. Rows whose send
 *       failed remain with {@code published_at = NULL} and will be retried next tick.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, DomainEvent<?>> kafkaTemplate;
    private final KafkaEventEnvelopeBuilder envelopeBuilder;
    private final BookingProperties properties;

    @Scheduled(fixedDelayString = "${naja7i.booking.outbox-poll-ms:2000}")
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
                // Leave the row un-published; the next tick will retry. Never crash the loop.
                log.warn("Outbox publish failed (will retry): id={} eventType={} cause={}",
                        row.getId(), row.getEventType(), ex.getMessage());
            }
        }
        if (sent > 0) {
            log.info("Outbox published {}/{} event(s)", sent, batch.size());
        }
    }
}

package ma.naja7i.chat.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.chat.domain.ProcessedEvent;
import ma.naja7i.chat.infrastructure.persistence.ProcessedEventRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Mongo-backed dedupe for Kafka consumers. Inserts a {@link ProcessedEvent} row keyed
 * by the inbound {@code eventId}; on duplicate-key violation we know a previous
 * delivery has already been handled.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotentEventGuard {

    private final ProcessedEventRepository repository;

    /**
     * @return {@code true} when this is the first time we see the event (caller may
     *         proceed); {@code false} on duplicates.
     */
    public boolean acquire(UUID eventId, String eventType) {
        if (eventId == null) return true;
        if (repository.existsById(eventId)) {
            log.debug("Duplicate event {} ({}) — skipping", eventId, eventType);
            return false;
        }
        try {
            repository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .processedAt(Instant.now())
                    .build());
            return true;
        } catch (DuplicateKeyException dup) {
            log.debug("Race on duplicate event {} ({}) — skipping", eventId, eventType);
            return false;
        }
    }
}

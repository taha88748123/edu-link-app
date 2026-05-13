package ma.naja7i.booking.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.booking.domain.ProcessedEvent;
import ma.naja7i.booking.infrastructure.persistence.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Inserts a row into {@code processed_events}; returns {@code false} if the event has
 * already been processed (PK violation). Used by Kafka consumers to make their handlers
 * idempotent under at-least-once delivery.
 */
@Component
@RequiredArgsConstructor
public class IdempotentEventGuard {

    private static final Logger log = LoggerFactory.getLogger(IdempotentEventGuard.class);

    private final ProcessedEventRepository processedEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessed(UUID eventId, String eventType) {
        try {
            processedEventRepository.saveAndFlush(ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .build());
            return true;
        } catch (DataIntegrityViolationException dup) {
            log.debug("Duplicate event {} ({}) — skipping", eventId, eventType);
            return false;
        }
    }
}

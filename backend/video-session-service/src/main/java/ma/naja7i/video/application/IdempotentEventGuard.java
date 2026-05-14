package ma.naja7i.video.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.video.domain.ProcessedEvent;
import ma.naja7i.video.infrastructure.persistence.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Inserts a row in {@code processed_events} returning {@code false} when the
 * event was already processed (PK violation). Used as the FIRST step in every
 * consumer transaction so duplicates are stopped before any side effect.
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

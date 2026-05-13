package ma.naja7i.notification.application;

import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.domain.ProcessedEvent;
import ma.naja7i.notification.infrastructure.persistence.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Guards Kafka consumers against duplicate delivery using {@code processed_events.event_id}
 * as the dedupe key.
 */
@Component
@Slf4j
public class IdempotencyGuard {

    private final ProcessedEventRepository repo;

    public IdempotencyGuard(ProcessedEventRepository repo) {
        this.repo = repo;
    }

    /**
     * @return {@code true} when the event has NOT been seen before (caller should
     *         process). {@code false} when the event is a duplicate (caller should skip).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean acquire(UUID eventId, String topic) {
        if (eventId == null) return true; // can't dedupe — let it through
        if (repo.existsById(eventId)) {
            log.debug("Duplicate event {} on {} — skipping", eventId, topic);
            return false;
        }
        try {
            repo.save(ProcessedEvent.builder().eventId(eventId).topic(topic).build());
            return true;
        } catch (DataIntegrityViolationException dup) {
            log.debug("Race on duplicate event {} on {} — skipping", eventId, topic);
            return false;
        }
    }
}

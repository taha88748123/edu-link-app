package ma.naja7i.calendar.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.calendar.domain.ProcessedEvent;
import ma.naja7i.calendar.domain.Slot;
import ma.naja7i.calendar.domain.SlotStatus;
import ma.naja7i.calendar.infrastructure.persistence.ProcessedEventRepository;
import ma.naja7i.calendar.infrastructure.persistence.SlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Centralised side-effects for {@code naja7i.profile.prof.validated.v1},
 * {@code naja7i.profile.prof.rejected.v1} and {@code naja7i.auth.user.suspended.v1}.
 *
 * <p>Each method is idempotent thanks to the {@code processed_events} dedup row
 * inserted in the same transaction.
 */
@Service
@RequiredArgsConstructor
public class ProfStatusService {

    private static final Logger log = LoggerFactory.getLogger(ProfStatusService.class);

    private final SlotRepository slotRepository;
    private final ProcessedEventRepository processedEventRepository;

    /** Returns {@code true} when the event was processed for the first time. */
    @Transactional
    public boolean handleProfValidated(UUID eventId, String eventType, UUID profId) {
        if (!markProcessed(eventId, eventType)) {
            return false;
        }
        // TODO(v2): consider materializing a default WEEKLY rule. For v1 the prof
        // creates their own rules via the REST API; we just acknowledge the event.
        log.info("prof.validated received for prof (hashed)={}, no slot action taken in v1",
                Integer.toHexString(profId.hashCode()));
        return true;
    }

    /** Returns the count of slots blocked, or {@code -1} if the event was a duplicate. */
    @Transactional
    public int handleProfRejectedOrSuspended(UUID eventId, String eventType, UUID profId) {
        if (!markProcessed(eventId, eventType)) {
            return -1;
        }
        List<Slot> future = slotRepository.findByProfIdAndStartAtGreaterThanAndStatusIn(
                profId, Instant.now(),
                List.of(SlotStatus.AVAILABLE, SlotStatus.RESERVED_PENDING));
        for (Slot s : future) {
            s.setStatus(SlotStatus.BLOCKED);
            s.setBookingId(null);
            s.setReservedUntil(null);
        }
        log.info("Blocked {} future slots after {}", future.size(), eventType);
        return future.size();
    }

    /**
     * Inserts a row into {@code processed_events}; returns {@code false} if the
     * event was already processed (PK violation).
     */
    private boolean markProcessed(UUID eventId, String eventType) {
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

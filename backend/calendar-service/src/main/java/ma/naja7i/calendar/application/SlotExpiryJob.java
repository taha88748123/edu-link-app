package ma.naja7i.calendar.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.calendar.domain.Slot;
import ma.naja7i.calendar.domain.SlotStatus;
import ma.naja7i.calendar.infrastructure.messaging.payload.SlotReleasedPayload;
import ma.naja7i.calendar.infrastructure.persistence.SlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Every 60 seconds, releases every slot whose {@code reserved_until} has elapsed —
 * the booking-service forgot to confirm/release the reservation in time.
 *
 * <p>Each released slot emits {@code naja7i.calendar.slot.released.v1} with
 * {@code reason=RESERVATION_EXPIRED}.
 */
@Component
@RequiredArgsConstructor
public class SlotExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(SlotExpiryJob.class);

    private final SlotRepository slotRepository;
    private final SlotService slotService;

    /** Cron format with seconds; default = top of every minute. */
    @Scheduled(cron = "${naja7i.calendar.expiry-scan-cron:0 * * * * *}")
    public void sweep() {
        Instant now = Instant.now();
        List<Slot> expired = slotRepository.findByStatusAndReservedUntilLessThan(
                SlotStatus.RESERVED_PENDING, now);
        if (expired.isEmpty()) return;
        int released = 0;
        for (Slot s : expired) {
            try {
                slotService.release(s.getId(), SlotReleasedPayload.REASON_RESERVATION_EXPIRED);
                released++;
            } catch (Exception ex) {
                // Another thread/job may have transitioned the slot between scan & release.
                log.debug("Skipping expired slot {}: {}", s.getId(), ex.getMessage());
            }
        }
        if (released > 0) {
            log.info("Released {} expired RESERVED_PENDING slot(s)", released);
        }
    }
}

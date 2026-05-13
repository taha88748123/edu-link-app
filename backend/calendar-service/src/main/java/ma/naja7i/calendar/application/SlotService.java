package ma.naja7i.calendar.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.calendar.config.CalendarProperties;
import ma.naja7i.calendar.domain.Slot;
import ma.naja7i.calendar.domain.SlotStatus;
import ma.naja7i.calendar.infrastructure.messaging.KafkaEventPublisher;
import ma.naja7i.calendar.infrastructure.messaging.payload.SlotBookedPayload;
import ma.naja7i.calendar.infrastructure.messaging.payload.SlotCreatedPayload;
import ma.naja7i.calendar.infrastructure.messaging.payload.SlotReleasedPayload;
import ma.naja7i.calendar.infrastructure.persistence.SlotRepository;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * State machine for {@link Slot}.
 *
 * <pre>
 *   AVAILABLE      -> RESERVED_PENDING  (reserve, internal)
 *   AVAILABLE      <-> BLOCKED          (block/unblock, PROF)
 *   RESERVED_PENDING -> BOOKED           (confirm, internal)
 *   RESERVED_PENDING -> AVAILABLE        (release: TTL or explicit)
 *   BOOKED         -> AVAILABLE          (release: cancellation)
 * </pre>
 *
 * <p>Concurrent reservations on the same slot lose with an
 * {@link ObjectOptimisticLockingFailureException} (via the {@code @Version} column),
 * which we translate to {@link ErrorCode#CALENDAR_SLOT_CONFLICT}.
 */
@Service
@RequiredArgsConstructor
public class SlotService {

    public static final int MAX_PAGE_SIZE = 200;

    private final SlotRepository slotRepository;
    private final KafkaEventPublisher publisher;
    private final CalendarProperties props;

    // ============================================================ queries

    @Transactional(readOnly = true)
    public Page<Slot> listAvailableForProf(UUID profId, Instant from, Instant to,
                                           Modality modality, int page, int size) {
        Pageable pageable = clamp(page, size);
        return slotRepository.findAvailableForProf(profId, from, to, modality, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Slot> listForOwner(UUID profId, Instant from, Instant to,
                                   SlotStatus status, int page, int size) {
        Pageable pageable = clamp(page, size);
        return slotRepository.findForProf(profId, from, to, status, pageable);
    }

    @Transactional(readOnly = true)
    public Slot get(UUID slotId) {
        return slotRepository.findById(slotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_SLOT_NOT_FOUND,
                        "Slot not found", Map.of("slotId", slotId)));
    }

    // ============================================================ PROF block / unblock

    /**
     * AVAILABLE → BLOCKED. Only the owning PROF may call this.
     * Reserved or booked slots are protected (would impact a live booking).
     */
    @Transactional
    public Slot block(UUID profId, UUID slotId) {
        Slot s = loadOwned(profId, slotId);
        if (s.getStatus() != SlotStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.CALENDAR_SLOT_CONFLICT,
                    "Only AVAILABLE slots can be blocked",
                    Map.of("slotId", slotId, "status", s.getStatus()));
        }
        s.setStatus(SlotStatus.BLOCKED);
        return slotRepository.save(s);
    }

    /**
     * BLOCKED → AVAILABLE. Re-emits {@code slot.created.v1} so the matching engine
     * picks it up again (the slot is conceptually new to the catalog).
     */
    @Transactional
    public Slot unblock(UUID profId, UUID slotId) {
        Slot s = loadOwned(profId, slotId);
        if (s.getStatus() != SlotStatus.BLOCKED) {
            throw new BusinessException(ErrorCode.CALENDAR_SLOT_CONFLICT,
                    "Only BLOCKED slots can be unblocked",
                    Map.of("slotId", slotId, "status", s.getStatus()));
        }
        s.setStatus(SlotStatus.AVAILABLE);
        Slot saved = slotRepository.save(s);
        publisher.publish(
                EventTopics.CALENDAR_SLOT_CREATED,
                saved.getProfId().toString(),
                EventTopics.CALENDAR_SLOT_CREATED,
                new SlotCreatedPayload(saved.getId(), saved.getProfId(),
                        saved.getStartAt(), saved.getEndAt(), saved.getModality()));
        return saved;
    }

    // ============================================================ internal lifecycle (booking-service)

    /**
     * AVAILABLE → RESERVED_PENDING. Sets {@code reservedUntil = now + ttl}.
     * Returns 409 if the slot is in any other state.
     */
    @Transactional
    public Slot reserve(UUID slotId, UUID bookingId) {
        Slot s = get(slotId);
        if (s.getStatus() != SlotStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.CALENDAR_SLOT_CONFLICT,
                    "Slot not available", Map.of("slotId", slotId, "status", s.getStatus()));
        }
        s.setStatus(SlotStatus.RESERVED_PENDING);
        s.setBookingId(bookingId);
        s.setReservedUntil(Instant.now().plusSeconds(props.getTtlMinutes() * 60L));
        try {
            return slotRepository.saveAndFlush(s);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessException(ErrorCode.CALENDAR_SLOT_CONFLICT,
                    "Slot was modified concurrently", Map.of("slotId", slotId));
        }
    }

    /**
     * RESERVED_PENDING → BOOKED. Emits {@code slot.booked.v1}.
     */
    @Transactional
    public Slot confirm(UUID slotId, UUID bookingId) {
        Slot s = get(slotId);
        if (s.getStatus() != SlotStatus.RESERVED_PENDING) {
            throw new BusinessException(ErrorCode.CALENDAR_SLOT_CONFLICT,
                    "Slot is not RESERVED_PENDING",
                    Map.of("slotId", slotId, "status", s.getStatus()));
        }
        // Optional safety: confirm must reference the same bookingId that reserved it.
        if (s.getBookingId() != null && !s.getBookingId().equals(bookingId)) {
            throw new BusinessException(ErrorCode.CALENDAR_SLOT_CONFLICT,
                    "Slot reserved by another booking",
                    Map.of("slotId", slotId));
        }
        s.setStatus(SlotStatus.BOOKED);
        s.setBookingId(bookingId);
        s.setReservedUntil(null);
        Slot saved;
        try {
            saved = slotRepository.saveAndFlush(s);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessException(ErrorCode.CALENDAR_SLOT_CONFLICT,
                    "Slot was modified concurrently", Map.of("slotId", slotId));
        }
        publisher.publish(
                EventTopics.CALENDAR_SLOT_BOOKED,
                saved.getProfId().toString(),
                EventTopics.CALENDAR_SLOT_BOOKED,
                new SlotBookedPayload(saved.getId(), saved.getBookingId()));
        return saved;
    }

    /**
     * RESERVED_PENDING|BOOKED → AVAILABLE. Emits {@code slot.released.v1}.
     */
    @Transactional
    public Slot release(UUID slotId, String reason) {
        Slot s = get(slotId);
        if (s.getStatus() != SlotStatus.RESERVED_PENDING && s.getStatus() != SlotStatus.BOOKED) {
            throw new BusinessException(ErrorCode.CALENDAR_SLOT_CONFLICT,
                    "Slot cannot be released from current status",
                    Map.of("slotId", slotId, "status", s.getStatus()));
        }
        s.setStatus(SlotStatus.AVAILABLE);
        s.setBookingId(null);
        s.setReservedUntil(null);
        Slot saved;
        try {
            saved = slotRepository.saveAndFlush(s);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BusinessException(ErrorCode.CALENDAR_SLOT_CONFLICT,
                    "Slot was modified concurrently", Map.of("slotId", slotId));
        }
        publisher.publish(
                EventTopics.CALENDAR_SLOT_RELEASED,
                saved.getProfId().toString(),
                EventTopics.CALENDAR_SLOT_RELEASED,
                new SlotReleasedPayload(saved.getId(),
                        reason == null ? SlotReleasedPayload.REASON_MANUAL : reason));
        return saved;
    }

    // ============================================================ helpers

    private Slot loadOwned(UUID profId, UUID slotId) {
        Slot s = get(slotId);
        if (!s.getProfId().equals(profId)) {
            // Treat as not-found to avoid IDOR enumeration leaks.
            throw new BusinessException(ErrorCode.CALENDAR_SLOT_NOT_FOUND,
                    "Slot not found", Map.of("slotId", slotId));
        }
        return s;
    }

    private Pageable clamp(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize);
    }
}

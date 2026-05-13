package ma.naja7i.booking.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.booking.config.BookingProperties;
import ma.naja7i.booking.domain.Booking;
import ma.naja7i.booking.domain.BookingStateTransition;
import ma.naja7i.booking.domain.OutboxEvent;
import ma.naja7i.booking.infrastructure.messaging.payload.BookingAcceptedPayload;
import ma.naja7i.booking.infrastructure.messaging.payload.BookingCancelledPayload;
import ma.naja7i.booking.infrastructure.messaging.payload.BookingConfirmedPayload;
import ma.naja7i.booking.infrastructure.messaging.payload.BookingDeclinedPayload;
import ma.naja7i.booking.infrastructure.messaging.payload.BookingRequestedPayload;
import ma.naja7i.booking.infrastructure.messaging.payload.BookingStartedPayload;
import ma.naja7i.booking.infrastructure.persistence.BookingRepository;
import ma.naja7i.booking.infrastructure.persistence.BookingStateTransitionRepository;
import ma.naja7i.booking.infrastructure.persistence.OutboxRepository;
import ma.naja7i.booking.infrastructure.rest.CalendarInternalClient;
import ma.naja7i.booking.infrastructure.rest.CalendarSlotView;
import ma.naja7i.booking.infrastructure.rest.PricingInternalClient;
import ma.naja7i.booking.infrastructure.rest.QuoteResult;
import ma.naja7i.booking.infrastructure.web.dto.CreateBookingRequest;
import ma.naja7i.shared.dto.BookingStatus;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * State machine for {@link Booking}.
 *
 * <pre>
 *   (caller=STUDENT) create     -> DEMANDEE
 *   (caller=PROF)    accept     -> ACCEPTEE_PAR_PROF
 *   (caller=PROF)    decline    -> ANNULEE (records decline_reason / declined_at)
 *   (caller=PROF)    start      -> EN_COURS
 *   (consumer)       end        -> TERMINEE (from naja7i.video.session.ended.v1)
 *   (caller=STUDENT) confirm    -> CONFIRMEE
 *   (caller=*)       cancel     -> ANNULEE  (allowed in DEMANDEE / ACCEPTEE_PAR_PROF)
 *   (caller=*)       dispute    -> LITIGE   (window enforced for CONFIRMEE)
 * </pre>
 *
 * <p>Each transition persists a {@link BookingStateTransition} audit row and an
 * {@link OutboxEvent} for downstream services. The outbox row is published by
 * {@link OutboxPoller}.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    public static final int MAX_PAGE_SIZE = 200;

    private final BookingRepository bookingRepository;
    private final BookingStateTransitionRepository transitionRepository;
    private final OutboxRepository outboxRepository;
    private final KafkaEventEnvelopeBuilder envelopeBuilder;
    private final CalendarInternalClient calendarClient;
    private final PricingInternalClient pricingClient;
    private final BookingProperties properties;

    // ================================================================ queries

    @Transactional(readOnly = true)
    public Booking get(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Booking not found", Map.of("bookingId", bookingId)));
    }

    @Transactional(readOnly = true)
    public Booking getOwned(UUID bookingId, UUID viewerId, Role viewerRole) {
        Booking b = get(bookingId);
        if (viewerRole == Role.ADMIN) return b;
        if (viewerRole == Role.STUDENT && b.getStudentId().equals(viewerId)) return b;
        if (viewerRole == Role.PROF && b.getProfId().equals(viewerId)) return b;
        // Mask the existence to avoid IDOR enumeration leaks.
        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                "Booking not found", Map.of("bookingId", bookingId));
    }

    @Transactional(readOnly = true)
    public Page<Booking> listForCaller(UUID viewerId, Role viewerRole, BookingStatus status,
                                       int page, int size) {
        Pageable pageable = clamp(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return switch (viewerRole) {
            case STUDENT -> bookingRepository.findForStudent(viewerId, status, pageable);
            case PROF    -> bookingRepository.findForProf(viewerId, status, pageable);
            default      -> Page.empty(pageable);
        };
    }

    @Transactional(readOnly = true)
    public Page<Booking> listForAdmin(BookingStatus status, Instant from, Instant to,
                                      int page, int size) {
        Pageable pageable = clamp(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return bookingRepository.findForAdmin(status, from, to, pageable);
    }

    // ================================================================ create (STUDENT)

    /**
     * Atomic create:
     * <ol>
     *   <li>{@code pricing-service} quote.</li>
     *   <li>{@code calendar-service} reserve.</li>
     *   <li>Persist booking + audit + outbox row.</li>
     * </ol>
     * If anything after step 2 throws, we best-effort release the calendar slot.
     */
    @Transactional
    public Booking create(UUID studentId, CreateBookingRequest req) {
        // 1. Quote
        Integer dow = null; // booking-service does not know the slot start until after :reserve,
                            // but pricing's day_of_week is optional (falls back). v2 may pass it.
        QuoteResult quote = pricingClient.quote(
                req.profId(), req.subjectCode(), req.level(), req.modality(),
                req.durationMinutes(), dow, req.packId());

        // 2. Reserve slot
        UUID bookingId = UUID.randomUUID();
        CalendarSlotView slot = calendarClient.reserveSlot(req.slotId(), bookingId);

        try {
            // 3. Persist booking
            Booking booking = Booking.builder()
                    .id(bookingId)
                    .studentId(studentId)
                    .profId(req.profId())
                    .slotId(req.slotId())
                    .subjectCode(req.subjectCode())
                    .level(req.level())
                    .modality(req.modality())
                    .durationMinutes(req.durationMinutes().shortValue())
                    .amountMinor(quote.baseAmountMinor())
                    .platformFeeMinor(quote.platformFeeMinor())
                    .totalAmountMinor(quote.totalAmountMinor())
                    .currency(quote.currency() == null ? "MAD" : quote.currency())
                    .status(BookingStatus.DEMANDEE)
                    .scheduledStart(slot.startAt())
                    .scheduledEnd(slot.endAt())
                    .build();
            Booking saved = bookingRepository.saveAndFlush(booking);

            recordTransition(saved.getId(), null, BookingStatus.DEMANDEE, studentId,
                    "Booking created");

            enqueueOutbox(saved.getId(), EventTopics.BOOKING_REQUESTED,
                    new BookingRequestedPayload(
                            saved.getId(), saved.getStudentId(), saved.getProfId(), saved.getSlotId(),
                            BigDecimal.valueOf(saved.getTotalAmountMinor()).movePointLeft(2),
                            saved.getTotalAmountMinor(), saved.getCurrency(),
                            saved.getScheduledStart()));
            return saved;
        } catch (RuntimeException ex) {
            // Best-effort: release the calendar reservation. The slot TTL job will sweep
            // it within 10min if this also fails.
            safeReleaseSlot(req.slotId(), "BOOKING_CREATE_FAILED");
            throw ex;
        }
    }

    // ================================================================ PROF accept / decline / start

    @Transactional
    public Booking accept(UUID bookingId, UUID profId) {
        Booking b = get(bookingId);
        if (!b.getProfId().equals(profId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE,
                    "Not your booking", Map.of("bookingId", bookingId));
        }
        if (b.getStatus() != BookingStatus.DEMANDEE) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "Booking is not in DEMANDEE",
                    Map.of("bookingId", bookingId, "status", b.getStatus()));
        }
        BookingStatus from = b.getStatus();
        b.setStatus(BookingStatus.ACCEPTEE_PAR_PROF);
        bookingRepository.saveAndFlush(b);
        // Confirm slot with calendar-service.
        calendarClient.confirmSlot(b.getSlotId(), b.getId());

        recordTransition(b.getId(), from, BookingStatus.ACCEPTEE_PAR_PROF, profId,
                "PROF accepted");
        enqueueOutbox(b.getId(), EventTopics.BOOKING_ACCEPTED,
                new BookingAcceptedPayload(b.getId(), Instant.now()));
        return b;
    }

    @Transactional
    public Booking decline(UUID bookingId, UUID profId, String reason) {
        Booking b = get(bookingId);
        if (!b.getProfId().equals(profId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE,
                    "Not your booking", Map.of("bookingId", bookingId));
        }
        if (b.getStatus() != BookingStatus.DEMANDEE) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "Booking is not in DEMANDEE",
                    Map.of("bookingId", bookingId, "status", b.getStatus()));
        }
        BookingStatus from = b.getStatus();
        Instant now = Instant.now();
        b.setStatus(BookingStatus.ANNULEE);
        b.setDeclineReason(reason);
        b.setDeclinedAt(now);
        b.setCancelledBy(profId);
        b.setCancelledAt(now);
        b.setCancelReason("DECLINED_BY_PROF");
        bookingRepository.saveAndFlush(b);

        safeReleaseSlot(b.getSlotId(), "BOOKING_DECLINED");
        recordTransition(b.getId(), from, BookingStatus.ANNULEE, profId, "PROF declined");
        enqueueOutbox(b.getId(), EventTopics.BOOKING_DECLINED,
                new BookingDeclinedPayload(b.getId(), reason));
        return b;
    }

    @Transactional
    public Booking start(UUID bookingId, UUID profId) {
        Booking b = get(bookingId);
        if (!b.getProfId().equals(profId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE,
                    "Not your booking", Map.of("bookingId", bookingId));
        }
        if (b.getStatus() != BookingStatus.ACCEPTEE_PAR_PROF) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "Booking is not in ACCEPTEE_PAR_PROF",
                    Map.of("bookingId", bookingId, "status", b.getStatus()));
        }
        // Lead tolerance: PROF can start up to leadToleranceMinutes before scheduled_start.
        Instant earliest = b.getScheduledStart()
                .minus(Duration.ofMinutes(properties.getLeadToleranceMinutes()));
        Instant now = Instant.now();
        if (now.isBefore(earliest)) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "Too early to start (scheduledStart=" + b.getScheduledStart() + ")",
                    Map.of("bookingId", bookingId,
                           "scheduledStart", b.getScheduledStart(),
                           "earliestStart", earliest));
        }
        BookingStatus from = b.getStatus();
        b.setStatus(BookingStatus.EN_COURS);
        b.setActualStart(now);
        bookingRepository.saveAndFlush(b);

        recordTransition(b.getId(), from, BookingStatus.EN_COURS, profId, "PROF started");
        enqueueOutbox(b.getId(), EventTopics.BOOKING_STARTED,
                new BookingStartedPayload(b.getId(), now));
        return b;
    }

    // ================================================================ STUDENT confirm

    @Transactional
    public Booking confirm(UUID bookingId, UUID studentId) {
        Booking b = get(bookingId);
        if (!b.getStudentId().equals(studentId)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE,
                    "Not your booking", Map.of("bookingId", bookingId));
        }
        if (b.getStatus() != BookingStatus.TERMINEE) {
            throw new BusinessException(ErrorCode.BOOKING_CONFIRM_NOT_TERMINATED,
                    "Booking is not in TERMINEE",
                    Map.of("bookingId", bookingId, "status", b.getStatus()));
        }
        BookingStatus from = b.getStatus();
        Instant now = Instant.now();
        b.setStatus(BookingStatus.CONFIRMEE);
        b.setConfirmedAt(now);
        bookingRepository.saveAndFlush(b);

        recordTransition(b.getId(), from, BookingStatus.CONFIRMEE, studentId, "STUDENT confirmed");
        enqueueOutbox(b.getId(), EventTopics.BOOKING_CONFIRMED,
                new BookingConfirmedPayload(b.getId(), now));
        return b;
    }

    // ================================================================ cancel (STUDENT or PROF)

    @Transactional
    public Booking cancel(UUID bookingId, UUID actorId, Role actorRole, String reason) {
        Booking b = get(bookingId);
        // Authorisation: only the booking's student/prof (or ADMIN) may cancel.
        boolean isAdmin = actorRole == Role.ADMIN;
        boolean isOwner = (actorRole == Role.STUDENT && b.getStudentId().equals(actorId))
                || (actorRole == Role.PROF && b.getProfId().equals(actorId));
        if (!isAdmin && !isOwner) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE,
                    "Not your booking", Map.of("bookingId", bookingId));
        }
        if (b.getStatus() != BookingStatus.DEMANDEE
                && b.getStatus() != BookingStatus.ACCEPTEE_PAR_PROF) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "Cancel only allowed in DEMANDEE / ACCEPTEE_PAR_PROF",
                    Map.of("bookingId", bookingId, "status", b.getStatus()));
        }
        // Cannot cancel a booking that has already started.
        if (Instant.now().isAfter(b.getScheduledStart())) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "Cannot cancel after scheduled_start",
                    Map.of("bookingId", bookingId, "scheduledStart", b.getScheduledStart()));
        }
        BookingStatus from = b.getStatus();
        Instant now = Instant.now();
        b.setStatus(BookingStatus.ANNULEE);
        b.setCancelledBy(actorId);
        b.setCancelledAt(now);
        b.setCancelReason(reason);
        bookingRepository.saveAndFlush(b);

        safeReleaseSlot(b.getSlotId(), "BOOKING_CANCELLED");
        recordTransition(b.getId(), from, BookingStatus.ANNULEE, actorId,
                reason == null ? "cancelled" : reason);
        enqueueOutbox(b.getId(), EventTopics.BOOKING_CANCELLED,
                new BookingCancelledPayload(b.getId(), actorId, reason));
        return b;
    }

    // ================================================================ consumed: video.session.ended

    /**
     * EN_COURS -> TERMINEE on receipt of {@code naja7i.video.session.ended.v1}.
     *
     * <p>Idempotent: caller should have dedup'd via {@code processed_events}; this
     * method additionally tolerates a TERMINEE booking (no-op).
     */
    @Transactional
    public void markEnded(UUID bookingId, long durationSec) {
        Booking b = bookingRepository.findById(bookingId).orElse(null);
        if (b == null) {
            log.warn("booking.ended received for unknown bookingId={}", bookingId);
            return;
        }
        if (b.getStatus() == BookingStatus.TERMINEE
                || b.getStatus() == BookingStatus.CONFIRMEE) {
            // Already-handled
            return;
        }
        if (b.getStatus() != BookingStatus.EN_COURS) {
            log.warn("booking.ended on unexpected status: bookingId={} status={}",
                    bookingId, b.getStatus());
            return;
        }
        BookingStatus from = b.getStatus();
        b.setStatus(BookingStatus.TERMINEE);
        Instant end = b.getActualStart() == null
                ? Instant.now()
                : b.getActualStart().plusSeconds(durationSec);
        b.setActualEnd(end);
        bookingRepository.saveAndFlush(b);
        recordTransition(b.getId(), from, BookingStatus.TERMINEE, null,
                "video session ended");
    }

    // ================================================================ consumed: auth.user.suspended

    /**
     * Cancels all open (DEMANDEE / ACCEPTEE_PAR_PROF) bookings of a suspended user.
     * Idempotent at the row level (re-running on already-ANNULEE rows is a no-op).
     */
    @Transactional
    public int cancelOpenBookingsOf(UUID userId) {
        List<Booking> open = bookingRepository.findOpenForStudent(userId,
                List.of(BookingStatus.DEMANDEE, BookingStatus.ACCEPTEE_PAR_PROF));
        int cancelled = 0;
        Instant now = Instant.now();
        for (Booking b : open) {
            BookingStatus from = b.getStatus();
            b.setStatus(BookingStatus.ANNULEE);
            b.setCancelReason("USER_SUSPENDED");
            b.setCancelledBy(userId); // self (auth event)
            b.setCancelledAt(now);
            bookingRepository.save(b);
            safeReleaseSlot(b.getSlotId(), "USER_SUSPENDED");
            recordTransition(b.getId(), from, BookingStatus.ANNULEE, userId, "USER_SUSPENDED");
            enqueueOutbox(b.getId(), EventTopics.BOOKING_CANCELLED,
                    new BookingCancelledPayload(b.getId(), userId, "USER_SUSPENDED"));
            cancelled++;
        }
        return cancelled;
    }

    // ================================================================ internals

    private void recordTransition(UUID bookingId, BookingStatus from, BookingStatus to,
                                  UUID triggeredBy, String reason) {
        transitionRepository.save(BookingStateTransition.builder()
                .bookingId(bookingId)
                .fromStatus(from)
                .toStatus(to)
                .triggeredBy(triggeredBy)
                .reason(reason)
                .build());
    }

    /** package-visible so DisputeService can re-use it. */
    void enqueueOutbox(UUID aggregateId, String eventType, Object payload) {
        OutboxEvent row = envelopeBuilder.toOutbox(
                aggregateId, eventType, aggregateId.toString(), payload);
        outboxRepository.save(row);
    }

    private void safeReleaseSlot(UUID slotId, String reason) {
        try {
            calendarClient.releaseSlot(slotId, reason);
        } catch (RuntimeException ex) {
            // Calendar service is the source of truth; failures here are non-fatal because
            // the slot's RESERVED_PENDING TTL will sweep it within 10 minutes.
            log.warn("Failed to release slot {} ({}): {}", slotId, reason, ex.getMessage());
        }
    }

    /**
     * Compute the ISO day-of-week (1=Monday..7=Sunday) of an instant in the Casablanca timezone.
     * Currently unused but kept for the v2 pricing call when we route the slot's actual weekday
     * back into the quote (we deliberately don't pass it in v1 because we don't know the slot
     * times until after :reserve).
     */
    @SuppressWarnings("unused")
    private static Integer dayOfWeek(Instant when) {
        if (when == null) return null;
        DayOfWeek d = ZonedDateTime.ofInstant(when, ZoneId.of("Africa/Casablanca")).getDayOfWeek();
        // pricing-service uses 0..6 (Mon..Sun)
        return d.getValue() - 1;
    }

    private Pageable clamp(int page, int size, Sort sort) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize, sort);
    }
}

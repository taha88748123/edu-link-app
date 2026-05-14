package ma.naja7i.chat.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.chat.domain.Conversation;
import ma.naja7i.chat.domain.ConversationStatus;
import ma.naja7i.chat.domain.UnreadCounts;
import ma.naja7i.chat.infrastructure.persistence.ConversationRepository;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Conversation lifecycle, lookups and ownership checks.
 *
 * <p><b>Creation flow</b> is idempotent on {@code bookingId} : both the
 * {@code internal} REST entry and the Kafka {@code booking.requested} consumer
 * funnel through {@link #createOrGet}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    public static final int MAX_PAGE_SIZE = 200;

    private final ConversationRepository conversationRepository;

    // ---------------------------------------------------------------- create

    /**
     * Returns the existing conversation for {@code bookingId} if any, otherwise creates one.
     * Idempotent under concurrent first-creation (we tolerate a {@link DuplicateKeyException}).
     */
    public Conversation createOrGet(UUID bookingId, UUID profId, UUID studentId,
                                    ConversationStatus initialStatus) {
        Optional<Conversation> existing = conversationRepository.findByBookingId(bookingId);
        if (existing.isPresent()) return existing.get();

        Conversation conv = Conversation.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .profId(profId)
                .studentId(studentId)
                .status(initialStatus == null ? ConversationStatus.PRE_ACCEPT : initialStatus)
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .unreadCounts(UnreadCounts.zero())
                .build();
        try {
            return conversationRepository.save(conv);
        } catch (DuplicateKeyException dup) {
            // Race with another creator — fetch the winner.
            return conversationRepository.findByBookingId(bookingId)
                    .orElseThrow(() -> dup);
        }
    }

    // ---------------------------------------------------------------- queries

    public Conversation get(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_CONVERSATION_NOT_FOUND,
                        "Conversation not found",
                        Map.of("conversationId", conversationId)));
    }

    public Optional<Conversation> findByBookingId(UUID bookingId) {
        return conversationRepository.findByBookingId(bookingId);
    }

    /**
     * Returns the conversation, throwing {@link ErrorCode#CHAT_CONVERSATION_NOT_FOUND}
     * with the same code (rather than 403) when the caller is not a participant —
     * this avoids IDOR enumeration.
     */
    public Conversation getOwned(UUID conversationId, UUID viewerId, Role viewerRole) {
        Conversation conv = get(conversationId);
        if (viewerRole == Role.ADMIN) return conv;
        if (!conv.isParticipant(viewerId)) {
            throw new BusinessException(ErrorCode.CHAT_CONVERSATION_NOT_FOUND,
                    "Conversation not found",
                    Map.of("conversationId", conversationId));
        }
        return conv;
    }

    public Page<Conversation> listForCaller(UUID viewerId, ConversationStatus status,
                                            int page, int size) {
        Pageable pageable = clamp(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        if (status != null) {
            return conversationRepository.findByProfIdOrStudentIdAndStatus(viewerId, viewerId, status, pageable);
        }
        return conversationRepository.findByProfIdOrStudentId(viewerId, viewerId, pageable);
    }

    public Page<Conversation> adminSearch(UUID bookingId, UUID profId, UUID studentId,
                                          int page, int size) {
        Pageable pageable = clamp(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        if (bookingId != null) {
            return conversationRepository.findByBookingId(bookingId, pageable);
        }
        if (profId != null) {
            return conversationRepository.findByProfId(profId, pageable);
        }
        if (studentId != null) {
            return conversationRepository.findByStudentId(studentId, pageable);
        }
        // Fall back to the full collection (use sparingly — admin debug only).
        return new PageImpl<>(conversationRepository.findAll(pageable.getSort()).stream().toList(),
                pageable, conversationRepository.count());
    }

    // ---------------------------------------------------------------- transitions

    /** PRE_ACCEPT -> POST_ACCEPT (triggered by booking.accepted). Idempotent. */
    public Conversation transitionToPostAccept(UUID bookingId) {
        return conversationRepository.findByBookingId(bookingId)
                .map(conv -> {
                    if (conv.getStatus() == ConversationStatus.POST_ACCEPT
                            || conv.getStatus() == ConversationStatus.CLOSED) {
                        return conv;
                    }
                    conv.setStatus(ConversationStatus.POST_ACCEPT);
                    return conversationRepository.save(conv);
                })
                .orElse(null);
    }

    /** Any state -> CLOSED (triggered by cancel/decline/confirm/suspend). Idempotent. */
    public Conversation close(UUID conversationId, String reason) {
        Conversation conv = get(conversationId);
        if (conv.getStatus() == ConversationStatus.CLOSED) return conv;
        conv.setStatus(ConversationStatus.CLOSED);
        conv.setClosedAt(Instant.now());
        log.info("Closing conversation {} (reason={})", conversationId, reason);
        return conversationRepository.save(conv);
    }

    public Conversation closeByBookingId(UUID bookingId, String reason) {
        return conversationRepository.findByBookingId(bookingId)
                .map(c -> close(c.getId(), reason))
                .orElse(null);
    }

    public int closeAllForUser(UUID userId, String reason) {
        List<Conversation> open = conversationRepository.findByProfIdOrStudentId(userId, userId);
        int closed = 0;
        for (Conversation c : open) {
            if (c.getStatus() == ConversationStatus.CLOSED) continue;
            c.setStatus(ConversationStatus.CLOSED);
            c.setClosedAt(Instant.now());
            conversationRepository.save(c);
            closed++;
        }
        if (closed > 0) {
            log.info("Closed {} conversation(s) for user (reason={})", closed, reason);
        }
        return closed;
    }

    // ---------------------------------------------------------------- unread bookkeeping

    /**
     * Reset the viewer's unread counter to zero and persist. Returns the updated conv.
     */
    public Conversation resetUnreadFor(UUID conversationId, UUID viewerId) {
        Conversation conv = get(conversationId);
        if (!conv.isParticipant(viewerId)) return conv;
        UnreadCounts counts = conv.getUnreadCounts() == null ? UnreadCounts.zero() : conv.getUnreadCounts();
        if (viewerId.equals(conv.getProfId())) {
            counts.setProfUnread(0);
        } else if (viewerId.equals(conv.getStudentId())) {
            counts.setStudentUnread(0);
        }
        conv.setUnreadCounts(counts);
        return conversationRepository.save(conv);
    }

    /** Increment the OTHER participant's unread counter when a message is posted. */
    public void incrementUnreadForOtherSide(Conversation conv, UUID senderId, Instant when) {
        UnreadCounts counts = conv.getUnreadCounts() == null ? UnreadCounts.zero() : conv.getUnreadCounts();
        if (senderId.equals(conv.getProfId())) {
            counts.setStudentUnread(counts.getStudentUnread() + 1);
        } else if (senderId.equals(conv.getStudentId())) {
            counts.setProfUnread(counts.getProfUnread() + 1);
        }
        conv.setUnreadCounts(counts);
        conv.setLastMessageAt(when);
        conversationRepository.save(conv);
    }

    private Pageable clamp(int page, int size, Sort sort) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize, sort);
    }
}

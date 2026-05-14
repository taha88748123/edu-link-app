package ma.naja7i.chat.infrastructure.web.dto;

import ma.naja7i.chat.domain.Conversation;
import ma.naja7i.chat.domain.ConversationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Public-facing projection of a {@link Conversation}. Carries only the fields a
 * participant or admin may legitimately see (no per-side {@code unreadCount} leak —
 * each viewer sees only their own counter).
 */
public record ConversationView(
        UUID id,
        UUID bookingId,
        UUID profId,
        UUID studentId,
        ConversationStatus status,
        Instant lastMessageAt,
        Instant createdAt,
        Instant closedAt,
        int unreadCount
) {

    public static ConversationView of(Conversation c, UUID viewerId) {
        int unread = 0;
        if (c.getUnreadCounts() != null) {
            if (viewerId != null && viewerId.equals(c.getProfId())) {
                unread = c.getUnreadCounts().getProfUnread();
            } else if (viewerId != null && viewerId.equals(c.getStudentId())) {
                unread = c.getUnreadCounts().getStudentUnread();
            }
        }
        return new ConversationView(
                c.getId(), c.getBookingId(), c.getProfId(), c.getStudentId(),
                c.getStatus(), c.getLastMessageAt(), c.getCreatedAt(), c.getClosedAt(),
                unread);
    }
}

package ma.naja7i.chat.infrastructure.web.dto;

import ma.naja7i.chat.domain.Attachment;
import ma.naja7i.chat.domain.Message;
import ma.naja7i.chat.domain.MessageType;
import ma.naja7i.chat.domain.Moderation;
import ma.naja7i.chat.domain.SenderRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Admin-only message view — includes the un-masked {@code bodyOriginal} field for
 * anti-bypass investigations. Must never be returned from public endpoints.
 */
public record AdminMessageView(
        UUID id,
        UUID conversationId,
        UUID bookingId,
        UUID senderId,
        SenderRole senderRole,
        MessageType type,
        String body,
        String bodyOriginal,
        List<Attachment> attachments,
        Moderation moderation,
        Instant createdAt
) {

    public static AdminMessageView of(Message m) {
        return new AdminMessageView(
                m.getId(), m.getConversationId(), m.getBookingId(), m.getSenderId(),
                m.getSenderRole(), m.getType(), m.getBody(), m.getBodyOriginal(),
                m.getAttachments(), m.getModeration(), m.getCreatedAt());
    }
}

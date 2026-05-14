package ma.naja7i.chat.infrastructure.web.dto;

import ma.naja7i.chat.domain.Attachment;
import ma.naja7i.chat.domain.Message;
import ma.naja7i.chat.domain.MessageType;
import ma.naja7i.chat.domain.ModerationStatus;
import ma.naja7i.chat.domain.SenderRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public projection of a message. Never carries {@code bodyOriginal} — that field
 * is admin-only and exposed by {@link AdminMessageView}.
 */
public record MessageView(
        UUID id,
        UUID conversationId,
        UUID senderId,
        SenderRole senderRole,
        MessageType type,
        String body,
        List<Attachment> attachments,
        ModerationView moderation,
        Instant createdAt
) {

    public static MessageView of(Message m) {
        return new MessageView(
                m.getId(), m.getConversationId(), m.getSenderId(), m.getSenderRole(),
                m.getType(), m.getBody(), m.getAttachments(),
                ModerationView.of(m.getModeration() == null ? null : m.getModeration().getStatus(),
                        m.getModeration() == null ? null : m.getModeration().getFlags()),
                m.getCreatedAt());
    }

    public record ModerationView(ModerationStatus status, List<String> flags) {
        public static ModerationView of(ModerationStatus status, List<String> flags) {
            return new ModerationView(status, flags);
        }
    }
}

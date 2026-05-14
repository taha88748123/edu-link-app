package ma.naja7i.chat.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.chat.domain.Attachment;
import ma.naja7i.chat.domain.Conversation;
import ma.naja7i.chat.domain.ConversationStatus;
import ma.naja7i.chat.domain.Message;
import ma.naja7i.chat.domain.MessageType;
import ma.naja7i.chat.domain.Moderation;
import ma.naja7i.chat.domain.ModerationStatus;
import ma.naja7i.chat.domain.ReadBy;
import ma.naja7i.chat.domain.SenderRole;
import ma.naja7i.chat.infrastructure.messaging.KafkaEventPublisher;
import ma.naja7i.chat.infrastructure.messaging.payload.ChatBypassFlaggedPayload;
import ma.naja7i.chat.infrastructure.messaging.payload.ChatMessagePostedPayload;
import ma.naja7i.chat.infrastructure.persistence.MessageRepository;
import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.errors.ErrorCode;
import ma.naja7i.shared.events.EventTopics;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Send, list, mark-read for chat messages.
 *
 * <p>{@link #send} is the meat of the anti-bypass story:
 * <ol>
 *   <li>Authorise sender as a conversation participant.</li>
 *   <li>Run {@link MessageModerator} (regex masking, only when {@code status=PRE_ACCEPT}).</li>
 *   <li>Persist the message with masked {@code body} and the original {@code bodyOriginal}.</li>
 *   <li>Update the conversation's {@code lastMessageAt} and increment the recipient's
 *       unread counter.</li>
 *   <li>Emit {@code chat.message.posted.v1}. If moderation flagged anything, emit
 *       {@code chat.bypass.flagged.v1} too.</li>
 *   <li>Hand off the original body to the async {@link BypassAnalyzer} for AI scoring.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 200;
    public static final int PREVIEW_LENGTH = 100;

    public static final String TOPIC_BYPASS_FLAGGED = "naja7i.chat.bypass.flagged.v1";

    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final MessageModerator moderator;
    private final BypassAnalyzer bypassAnalyzer;
    private final KafkaEventPublisher kafkaPublisher;

    // ---------------------------------------------------------------- send

    public Message send(UUID conversationId, UUID senderId, Role senderRole,
                        MessageType type, String body, List<Attachment> attachments) {
        if (senderRole == Role.ADMIN) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN_ROLE,
                    "Admins do not post in chat conversations",
                    Map.of("conversationId", conversationId));
        }
        Conversation conv = conversationService.get(conversationId);
        if (!conv.isParticipant(senderId)) {
            throw new BusinessException(ErrorCode.CHAT_CONVERSATION_NOT_FOUND,
                    "Conversation not found",
                    Map.of("conversationId", conversationId));
        }
        if (conv.getStatus() == ConversationStatus.CLOSED) {
            throw new BusinessException(ErrorCode.BOOKING_INVALID_TRANSITION,
                    "Conversation is closed",
                    Map.of("conversationId", conversationId));
        }

        ModeratorResult mod = moderator.moderate(body, conv.getStatus());
        Instant now = Instant.now();

        SenderRole sRole = senderId.equals(conv.getProfId()) ? SenderRole.PROF : SenderRole.STUDENT;

        Message msg = Message.builder()
                .id(UUID.randomUUID())
                .conversationId(conv.getId())
                .bookingId(conv.getBookingId())
                .senderId(senderId)
                .senderRole(sRole)
                .type(type == null ? MessageType.TEXT : type)
                .body(mod.getSanitizedBody())
                .bodyOriginal(mod.getOriginalBody())
                .attachments(attachments == null ? new ArrayList<>() : attachments)
                .moderation(Moderation.builder()
                        .status(mod.getStatus())
                        .flags(mod.getFlags())
                        .maskedFields(mod.getMaskedFields())
                        .build())
                .readBy(new ArrayList<>(List.of(ReadBy.builder().userId(senderId).at(now).build())))
                .createdAt(now)
                .build();
        Message saved = messageRepository.save(msg);

        conversationService.incrementUnreadForOtherSide(conv, senderId, now);

        // Emit message.posted.v1 to Kafka. Preview = first 100 chars of the SANITIZED body.
        String preview = preview(saved.getBody());
        kafkaPublisher.publish(
                EventTopics.CHAT_MESSAGE_POSTED,
                conv.getId().toString(),
                EventTopics.CHAT_MESSAGE_POSTED,
                new ChatMessagePostedPayload(saved.getId(), conv.getId(), senderId,
                        saved.getType().name(), preview, now));

        // If moderation produced flags, also emit a bypass.flagged.v1 event.
        if (mod.wasModified()) {
            String severity = mod.getStatus() == ModerationStatus.MASKED ? "MEDIUM" : "HIGH";
            kafkaPublisher.publish(TOPIC_BYPASS_FLAGGED, conv.getId().toString(),
                    TOPIC_BYPASS_FLAGGED,
                    new ChatBypassFlaggedPayload(saved.getId(), conv.getId(), senderId,
                            mod.getFlags(), severity));
            log.info("Bypass flagged messageId={} conversationId={} flags={}",
                    saved.getId(), conv.getId(), mod.getFlags());
        }

        // Hand off to the async analyser (logging or anthropic). The analyser is
        // responsible for emitting any further events / updating the moderation row.
        bypassAnalyzer.analyse(saved.getId(), conv.getId(), senderId, mod.getOriginalBody());

        return saved;
    }

    // ---------------------------------------------------------------- list / read

    public List<Message> listMessages(UUID conversationId, UUID viewerId, Role viewerRole,
                                      UUID before, int size) {
        // Authorise.
        conversationService.getOwned(conversationId, viewerId, viewerRole);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        if (before == null) {
            return messageRepository.findByConversationIdOrderByCreatedAtDesc(
                    conversationId, PageRequest.of(0, safeSize));
        }
        Message cursor = messageRepository.findById(before)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Cursor message not found", Map.of("messageId", before)));
        return messageRepository.findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                conversationId, cursor.getCreatedAt(), PageRequest.of(0, safeSize));
    }

    /**
     * Append read receipts for every message in the conversation up to the cutoff
     * defined by {@code lastReadMessageId}. Resets the unread counter for the viewer.
     */
    public void markRead(UUID conversationId, UUID viewerId, Role viewerRole, UUID lastReadMessageId) {
        Conversation conv = conversationService.getOwned(conversationId, viewerId, viewerRole);
        if (!conv.isParticipant(viewerId)) return;

        Message cutoffMsg = messageRepository.findById(lastReadMessageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Message not found", Map.of("messageId", lastReadMessageId)));
        if (!cutoffMsg.getConversationId().equals(conversationId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Message does not belong to this conversation",
                    Map.of("conversationId", conversationId, "messageId", lastReadMessageId));
        }
        Instant now = Instant.now();
        List<Message> uptoCutoff = messageRepository
                .findByConversationIdAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                        conversationId, cutoffMsg.getCreatedAt());
        for (Message m : uptoCutoff) {
            if (m.getReadBy() == null) m.setReadBy(new ArrayList<>());
            boolean already = m.getReadBy().stream().anyMatch(r -> viewerId.equals(r.getUserId()));
            if (!already) {
                m.getReadBy().add(ReadBy.builder().userId(viewerId).at(now).build());
                messageRepository.save(m);
            }
        }
        conversationService.resetUnreadFor(conversationId, viewerId);
    }

    public Message getForAdmin(UUID messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Message not found", Map.of("messageId", messageId)));
    }

    private static String preview(String body) {
        if (body == null) return "";
        return body.length() <= PREVIEW_LENGTH ? body : body.substring(0, PREVIEW_LENGTH);
    }
}

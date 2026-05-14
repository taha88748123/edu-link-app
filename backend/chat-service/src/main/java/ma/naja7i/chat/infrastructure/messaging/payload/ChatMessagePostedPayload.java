package ma.naja7i.chat.infrastructure.messaging.payload;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload of {@code naja7i.chat.message.posted.v1}.
 *
 * <p>{@code preview} is the first 100 characters of the (already moderated) body.
 * Never carry full body or attachment URLs over Kafka — consumers fetch via REST
 * if they need more.</p>
 */
public record ChatMessagePostedPayload(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        String type,
        String preview,
        Instant createdAt
) {
}

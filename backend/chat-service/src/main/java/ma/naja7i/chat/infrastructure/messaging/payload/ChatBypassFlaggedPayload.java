package ma.naja7i.chat.infrastructure.messaging.payload;

import java.util.List;
import java.util.UUID;

/**
 * Payload of {@code naja7i.chat.bypass.flagged.v1}.
 * Consumed by admin-dashboard (and later: auth-service for suspension).
 */
public record ChatBypassFlaggedPayload(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        List<String> flags,
        /** LOW | MEDIUM | HIGH | CRITICAL */
        String severity
) {
}

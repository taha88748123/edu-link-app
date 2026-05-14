package ma.naja7i.chat.application;

import java.util.UUID;

/**
 * Asynchronous bypass-intent analyser. Implementations decide whether a message is
 * trying to circumvent the platform (e.g. soft-spelled phone numbers, paraphrased
 * "DM me on instagram", etc.).
 *
 * <p>Two flavours :
 * <ul>
 *   <li>{@code LoggingBypassAnalyzer} (default) — no-op, just logs.</li>
 *   <li>{@code ClaudeBypassAnalyzer} (activated by {@code naja7i.chat.moderation.driver=anthropic})
 *       — calls Anthropic's Messages API; falls back to logging on any error.</li>
 * </ul>
 */
public interface BypassAnalyzer {

    /**
     * Inspect a message asynchronously and, if appropriate, update its moderation
     * record. Implementations MUST swallow all exceptions and never call back to
     * the HTTP transaction that produced this analysis.
     *
     * @param messageId      Mongo {@code _id} of the message
     * @param conversationId conversation it belongs to
     * @param senderId       author of the message
     * @param originalBody   un-redacted body (admin-only)
     */
    void analyse(UUID messageId, UUID conversationId, UUID senderId, String originalBody);
}

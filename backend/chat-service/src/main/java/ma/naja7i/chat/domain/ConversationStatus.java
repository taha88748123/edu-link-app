package ma.naja7i.chat.domain;

/**
 * Lifecycle of a chat conversation:
 * <ul>
 *   <li>{@code PRE_ACCEPT} — booking is requested but not yet accepted; messages are
 *       moderated and contact patterns (phone/email/social handles) are masked.</li>
 *   <li>{@code POST_ACCEPT} — booking is accepted; participants may freely exchange
 *       contact info.</li>
 *   <li>{@code CLOSED} — booking is cancelled / declined / confirmed beyond grace
 *       period, or a participant has been suspended. New messages are rejected (409).</li>
 * </ul>
 */
public enum ConversationStatus {
    PRE_ACCEPT, POST_ACCEPT, CLOSED
}

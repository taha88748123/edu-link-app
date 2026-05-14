package ma.naja7i.chat.infrastructure.ws;

import ma.naja7i.chat.application.ConversationService;
import ma.naja7i.chat.domain.Conversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guards STOMP {@code SUBSCRIBE} frames: a subscription to
 * {@code /topic/conversations/{id}/messages} is only allowed when the
 * authenticated principal is a participant of that conversation (or ADMIN).
 *
 * <p>Rejections raise an {@link IllegalArgumentException} which Spring
 * translates into a STOMP {@code ERROR} frame.
 */
public class SubscriptionAuthorizationInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionAuthorizationInterceptor.class);
    private static final Pattern CONV_DEST = Pattern.compile(
            "^/topic/conversations/([0-9a-fA-F\\-]{36})/messages$");

    private final ConversationService conversations;

    public SubscriptionAuthorizationInterceptor(ConversationService conversations) {
        this.conversations = conversations;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }
        if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }
        String dest = accessor.getDestination();
        if (dest == null) {
            return message;
        }
        Matcher m = CONV_DEST.matcher(dest);
        if (!m.matches()) {
            // Other destinations (e.g. /user/queue/**) are governed by Spring's user-destination plumbing.
            return message;
        }
        StompPrincipal principal = (accessor.getUser() instanceof StompPrincipal sp)
                ? sp
                : (accessor.getSessionAttributes() == null
                        ? null
                        : (StompPrincipal) accessor.getSessionAttributes().get(JwtStompInterceptor.SESSION_PRINCIPAL_ATTR));
        if (principal == null) {
            log.debug("STOMP SUBSCRIBE rejected: no principal on session");
            throw new IllegalArgumentException("WS_SUBSCRIBE_FORBIDDEN");
        }
        UUID convId;
        try {
            convId = UUID.fromString(m.group(1));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("WS_SUBSCRIBE_INVALID_DEST");
        }
        try {
            Conversation conv = conversations.get(convId);
            boolean allowed = principal.role() != null
                    && (principal.role().name().equals("ADMIN") || conv.isParticipant(principal.userId()));
            if (!allowed) {
                log.debug("STOMP SUBSCRIBE rejected: user {} not a participant of {}",
                        principal.userId(), convId);
                throw new IllegalArgumentException("WS_SUBSCRIBE_FORBIDDEN");
            }
            return message;
        } catch (RuntimeException ex) {
            log.debug("STOMP SUBSCRIBE rejected on {}: {}", convId, ex.getMessage());
            throw new IllegalArgumentException("WS_SUBSCRIBE_FORBIDDEN");
        }
    }
}

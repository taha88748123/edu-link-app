package ma.naja7i.chat.infrastructure.ws;

import ma.naja7i.shared.errors.BusinessException;
import ma.naja7i.shared.security.JwtClaims;
import ma.naja7i.shared.security.JwtService;
import ma.naja7i.shared.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * STOMP {@link ChannelInterceptor} that authenticates the {@code CONNECT} frame
 * using the JWT carried in the {@code Authorization: Bearer …} STOMP header.
 *
 * <p>On success the resolved {@link StompPrincipal} is attached to the STOMP
 * session via {@link StompHeaderAccessor#setUser(java.security.Principal)} so
 * subsequent SEND / SUBSCRIBE frames carry the same identity.
 *
 * <p>Rejects with a runtime exception (translates to a STOMP {@code ERROR}
 * frame back to the client) when the token is missing, malformed, expired or
 * signed with the wrong secret. The client receives no extra detail to avoid
 * oracle-style timing attacks.
 */
public class JwtStompInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtStompInterceptor.class);

    private final JwtService jwtService;

    public JwtStompInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // Prefer the mutable accessor attached when the inbound STOMP frame was
        // built (Spring leaves it mutable for the inbound channel chain). Falls
        // back to wrap() for hand-built test messages.
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }
        if (accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }
        String auth = accessor.getFirstNativeHeader(JwtClaims.HEADER_AUTHORIZATION);
        if (auth == null || !auth.startsWith(JwtClaims.BEARER_PREFIX)) {
            log.debug("STOMP CONNECT rejected: missing Authorization header");
            throw new IllegalArgumentException("WS_AUTH_MISSING");
        }
        String token = auth.substring(JwtClaims.BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("WS_AUTH_MISSING");
        }
        try {
            UserContext ctx = jwtService.verify(token);
            StompPrincipal principal = new StompPrincipal(ctx.userId(), ctx.role(), ctx.locale());
            accessor.setUser(principal);
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put(SESSION_PRINCIPAL_ATTR, principal);
            }
            log.debug("STOMP CONNECT accepted userId={} role={}", principal.userId(), principal.role());
            // Re-emit the message so the new principal survives the channel chain.
            return org.springframework.messaging.support.MessageBuilder
                    .createMessage(message.getPayload(), accessor.getMessageHeaders());
        } catch (BusinessException | IllegalArgumentException e) {
            log.debug("STOMP CONNECT rejected: {}", e.getMessage());
            throw new IllegalArgumentException("WS_AUTH_INVALID");
        }
    }

    /** Session-attribute key used to recover the principal on later frames. */
    public static final String SESSION_PRINCIPAL_ATTR = "naja7i.stomp.principal";
}

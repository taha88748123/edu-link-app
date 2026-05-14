package ma.naja7i.chat.infrastructure.ws;

import ma.naja7i.shared.dto.Role;

import java.security.Principal;
import java.util.UUID;

/**
 * Authenticated WebSocket session principal carried through the STOMP session.
 *
 * <p>Populated by {@link JwtStompInterceptor} on CONNECT, then read by
 * {@link SubscriptionAuthorizationInterceptor} on SUBSCRIBE and by message
 * controllers to identify the caller.
 */
public final class StompPrincipal implements Principal {

    private final UUID userId;
    private final Role role;
    private final String locale;

    public StompPrincipal(UUID userId, Role role, String locale) {
        this.userId = userId;
        this.role = role;
        this.locale = locale;
    }

    public UUID userId() { return userId; }
    public Role role() { return role; }
    public String locale() { return locale; }

    @Override
    public String getName() {
        // Spring uses Principal#getName as the user destination key (e.g. /user/queue/...).
        return userId.toString();
    }
}

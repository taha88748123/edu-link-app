package ma.naja7i.notification.application;

import ma.naja7i.notification.domain.Channel;
import ma.naja7i.shared.dto.Role;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves the ordered list of {@link Channel}s for a given user role. v1 ships
 * a simple in-memory map — PROFs prefer WhatsApp, STUDENTs prefer Push (per the brief).
 *
 * <p>Once {@code user-profile-service} exposes {@code notification_prefs}, this resolver
 * will fetch them via REST and fall back to the map below if unavailable.</p>
 */
@Component
public class PreferenceResolver {

    /** Default channels when the role isn't known (e.g. admin alerts). */
    private static final List<Channel> DEFAULT_ORDER =
            List.of(Channel.WA, Channel.PUSH, Channel.SMS, Channel.EMAIL);

    public List<Channel> resolve(Role role) {
        if (role == null) return DEFAULT_ORDER;
        return switch (role) {
            case PROF, PARENT  -> List.of(Channel.WA, Channel.PUSH, Channel.SMS, Channel.EMAIL);
            case STUDENT       -> List.of(Channel.PUSH, Channel.WA, Channel.SMS, Channel.EMAIL);
            case ADMIN         -> List.of(Channel.EMAIL, Channel.WA);
        };
    }

    /** Pick the highest-priority channel for the role (first non-null hit). */
    public Channel preferred(Role role) {
        return resolve(role).get(0);
    }
}

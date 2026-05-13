package ma.naja7i.auth.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Short-lived password reset tokens — value = userId, TTL 30min.
 */
@Component
@RequiredArgsConstructor
public class PasswordResetTokenStore {

    private static final String KEY = "auth:pwreset:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;

    public void store(String token, UUID userId) {
        redis.opsForValue().set(KEY + token, userId.toString(), TTL);
    }

    public Optional<UUID> consume(String token) {
        String value = redis.opsForValue().getAndDelete(KEY + token);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public Duration ttl() {
        return TTL;
    }
}

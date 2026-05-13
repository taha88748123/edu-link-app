package ma.naja7i.auth.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import ma.naja7i.auth.config.OtpProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * OTP persistence in Redis.
 * - Hash stored under {@code auth:otp:{userId}}
 * - Attempt counter under {@code auth:otp:attempts:{userId}}
 *
 * Codes are stored as bcrypt hashes (never plain).
 */
@Component
@RequiredArgsConstructor
public class OtpStore {

    private static final String HASH_KEY     = "auth:otp:";
    private static final String ATTEMPTS_KEY = "auth:otp:attempts:";

    private final StringRedisTemplate redis;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties otpProps;

    public void store(UUID userId, String plainCode) {
        String hash = passwordEncoder.encode(plainCode);
        redis.opsForValue().set(HASH_KEY + userId, hash, otpProps.getTtl());
        redis.delete(ATTEMPTS_KEY + userId);
    }

    public Optional<String> findHash(UUID userId) {
        return Optional.ofNullable(redis.opsForValue().get(HASH_KEY + userId));
    }

    public Duration getRemainingTtl(UUID userId) {
        Long secs = redis.getExpire(HASH_KEY + userId, TimeUnit.SECONDS);
        return secs == null || secs < 0 ? Duration.ZERO : Duration.ofSeconds(secs);
    }

    public int incrementAttempts(UUID userId) {
        Long count = redis.opsForValue().increment(ATTEMPTS_KEY + userId);
        if (count != null && count == 1L) {
            redis.expire(ATTEMPTS_KEY + userId, otpProps.getTtl());
        }
        return count == null ? 0 : count.intValue();
    }

    public void clear(UUID userId) {
        redis.delete(HASH_KEY + userId);
        redis.delete(ATTEMPTS_KEY + userId);
    }

    public boolean verify(UUID userId, String plainCode) {
        return findHash(userId)
                .map(hash -> passwordEncoder.matches(plainCode, hash))
                .orElse(false);
    }
}

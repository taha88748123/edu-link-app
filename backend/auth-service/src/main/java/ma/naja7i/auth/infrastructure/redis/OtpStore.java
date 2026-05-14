package ma.naja7i.auth.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import ma.naja7i.auth.config.OtpProperties;
import org.springframework.beans.factory.annotation.Value;
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
 * - Plain code (TEST MODE ONLY) stored under {@code auth:otp:plain:{userId}}
 *
 * Codes are normally stored as bcrypt hashes (never plain). When
 * {@code naja7i.auth.dev.test-mode=true} is enabled, an extra companion key
 * carrying the plain code is written alongside the hash so the dev-only
 * /api/v1/dev/auth/otp/{userId} endpoint can read it. This MUST never be
 * enabled in production — see {@code TestSupportController}.
 */
@Component
@RequiredArgsConstructor
public class OtpStore {

    private static final String HASH_KEY     = "auth:otp:";
    private static final String ATTEMPTS_KEY = "auth:otp:attempts:";
    /** TEST-MODE ONLY: plain-code companion key used by the e2e backdoor. */
    public static final String PLAIN_KEY    = "auth:otp:plain:";

    private final StringRedisTemplate redis;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties otpProps;

    @Value("${naja7i.auth.dev.test-mode:false}")
    private boolean testMode;

    public void store(UUID userId, String plainCode) {
        String hash = passwordEncoder.encode(plainCode);
        redis.opsForValue().set(HASH_KEY + userId, hash, otpProps.getTtl());
        redis.delete(ATTEMPTS_KEY + userId);
        if (testMode) {
            // Companion key — only written when test mode is explicitly enabled.
            redis.opsForValue().set(PLAIN_KEY + userId, plainCode, otpProps.getTtl());
        }
    }

    /** TEST-MODE ONLY: returns the plain code if test-mode was enabled when stored. */
    public Optional<String> findPlainCode(UUID userId) {
        if (!testMode) return Optional.empty();
        return Optional.ofNullable(redis.opsForValue().get(PLAIN_KEY + userId));
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
        if (testMode) {
            redis.delete(PLAIN_KEY + userId);
        }
    }

    public boolean verify(UUID userId, String plainCode) {
        return findHash(userId)
                .map(hash -> passwordEncoder.matches(plainCode, hash))
                .orElse(false);
    }
}

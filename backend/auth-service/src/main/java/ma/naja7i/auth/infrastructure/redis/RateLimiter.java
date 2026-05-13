package ma.naja7i.auth.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import ma.naja7i.auth.config.OtpProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Sliding-window-ish rate limiter using Redis INCR + EXPIRE.
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate redis;
    private final OtpProperties otpProps;

    /**
     * Returns true if the {@code key} is below the configured threshold (and increments it),
     * false if the call must be rejected for rate-limiting.
     */
    public boolean tryAcquireOtpResend(String key) {
        String redisKey = "auth:otp:rate:" + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count == null) return true;
        if (count == 1L) {
            redis.expire(redisKey, Duration.ofMinutes(otpProps.getRateLimitWindowMinutes()));
        }
        return count <= otpProps.getRateLimitMaxRequests();
    }
}

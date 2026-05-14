package ma.naja7i.chat.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Tiny façade over Spring Data's {@link StringRedisTemplate} for the operations
 * used by chat (presence, typing, strike tracking). Keeps the call sites obvious
 * and lets tests substitute a fake easily.
 */
@Component
@RequiredArgsConstructor
public class RedisOps {

    private final StringRedisTemplate template;

    // -------- presence

    public void setPresence(String userId, Duration ttl) {
        template.opsForValue().set("chat:presence:" + userId, "1", ttl);
    }

    public boolean isOnline(String userId) {
        Boolean exists = template.hasKey("chat:presence:" + userId);
        return Boolean.TRUE.equals(exists);
    }

    public Long presenceTtlSeconds(String userId) {
        Long ttl = template.getExpire("chat:presence:" + userId);
        return ttl == null || ttl < 0 ? null : ttl;
    }

    // -------- typing

    /** Adds {@code userId} to the typing set; resets the TTL. */
    public void markTyping(String conversationId, String userId, Duration ttl) {
        String key = "chat:typing:" + conversationId;
        template.opsForSet().add(key, userId);
        template.expire(key, ttl);
    }

    public Set<String> typingUsers(String conversationId) {
        return template.opsForSet().members("chat:typing:" + conversationId);
    }

    // -------- bypass strikes (ZSET sliding window)

    /** Adds a strike at the current timestamp and returns the number of strikes within the window. */
    public long addBypassStrike(String userId, long nowEpochMs, long windowMs) {
        String key = "chat:bypass:strikes:" + userId;
        template.opsForZSet().add(key, nowEpochMs + ":" + nowEpochMs, nowEpochMs);
        // Drop entries older than now-window.
        template.opsForZSet().removeRangeByScore(key, 0, nowEpochMs - windowMs);
        Long count = template.opsForZSet().zCard(key);
        template.expire(key, Duration.ofMillis(windowMs));
        return count == null ? 0L : count;
    }
}

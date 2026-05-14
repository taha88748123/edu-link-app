package ma.naja7i.chat.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.chat.config.ChatProperties;
import ma.naja7i.chat.infrastructure.redis.RedisOps;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks "bypass strikes" per user using a rolling-window Redis ZSET. Each call to
 * {@link #recordStrike(UUID)} appends a timestamped entry and returns the count of
 * strikes that fall inside the window. The caller decides what to do on threshold
 * (typically: emit a CRITICAL {@code chat.bypass.flagged.v1} event).
 */
@Component
@RequiredArgsConstructor
public class BypassStrikeTracker {

    private final RedisOps redisOps;
    private final ChatProperties props;

    public StrikeOutcome recordStrike(UUID userId) {
        long now = Instant.now().toEpochMilli();
        long windowMs = props.getBypass().getStrikeWindowHours() * 3_600_000L;
        long count = redisOps.addBypassStrike(userId.toString(), now, windowMs);
        boolean escalate = count >= props.getBypass().getStrikeThreshold();
        return new StrikeOutcome(count, escalate);
    }

    public record StrikeOutcome(long strikesInWindow, boolean shouldEscalate) {}
}

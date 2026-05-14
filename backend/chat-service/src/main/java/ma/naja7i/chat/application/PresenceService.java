package ma.naja7i.chat.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.chat.config.ChatProperties;
import ma.naja7i.chat.infrastructure.redis.RedisOps;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Encapsulates presence (online indicator) and typing-indicator semantics. The
 * heartbeat is expected to be called by the gateway / frontend at a cadence smaller
 * than the configured TTL.
 */
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final RedisOps redisOps;
    private final ChatProperties props;

    public void heartbeat(UUID userId) {
        redisOps.setPresence(userId.toString(),
                Duration.ofSeconds(props.getPresence().getTtlSeconds()));
    }

    public boolean isOnline(UUID userId) {
        return redisOps.isOnline(userId.toString());
    }

    public Long presenceTtlSeconds(UUID userId) {
        return redisOps.presenceTtlSeconds(userId.toString());
    }

    public void markTyping(UUID conversationId, UUID userId) {
        redisOps.markTyping(conversationId.toString(), userId.toString(),
                Duration.ofSeconds(props.getTyping().getTtlSeconds()));
    }
}

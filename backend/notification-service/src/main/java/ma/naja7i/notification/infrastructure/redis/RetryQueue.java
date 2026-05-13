package ma.naja7i.notification.infrastructure.redis;

import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.config.NotificationProperties;
import ma.naja7i.notification.domain.Channel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

/**
 * Redis-backed retry queue (one sorted-set per channel : {@code notif:retry:{CHANNEL}}).
 * Score = epoch-millis of next-attempt time ; member = notification id.
 *
 * <p>The {@link ma.naja7i.notification.application.RetryScheduler} drains due members
 * every 10 seconds via {@code @Scheduled}.</p>
 */
@Component
@Slf4j
public class RetryQueue {

    private static final String KEY_PREFIX = "notif:retry:";

    private final StringRedisTemplate redis;
    private final NotificationProperties props;

    public RetryQueue(StringRedisTemplate redis, NotificationProperties props) {
        this.redis = redis;
        this.props = props;
    }

    /** Schedule a retry. Returns true if scheduled, false when max attempts reached. */
    public boolean schedule(Long notificationId, Channel channel, int currentAttempts) {
        long[] backoffs = props.getRetry().getBackoffSeconds();
        if (currentAttempts >= props.getRetry().getMaxAttempts()) {
            log.warn("Retry budget exhausted for id={} channel={} (attempts={})",
                    notificationId, channel, currentAttempts);
            return false;
        }
        long delaySec = backoffs[Math.min(currentAttempts, backoffs.length - 1)];
        long score = Instant.now().toEpochMilli() + (delaySec * 1000);
        redis.opsForZSet().add(KEY_PREFIX + channel.name(), String.valueOf(notificationId), score);
        log.debug("Scheduled retry id={} channel={} in {}s", notificationId, channel, delaySec);
        return true;
    }

    /** Drain members whose score &le; now. Caller is responsible for re-dispatch. */
    public Set<ZSetOperations.TypedTuple<String>> pollDue(Channel channel, int max) {
        long now = Instant.now().toEpochMilli();
        return redis.opsForZSet().rangeByScoreWithScores(KEY_PREFIX + channel.name(), 0, now, 0, max);
    }

    public void remove(Channel channel, String member) {
        redis.opsForZSet().remove(KEY_PREFIX + channel.name(), member);
    }

    public long size(Channel channel) {
        Long s = redis.opsForZSet().zCard(KEY_PREFIX + channel.name());
        return s == null ? 0 : s;
    }
}

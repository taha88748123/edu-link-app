package ma.naja7i.notification.application;

import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.Notification;
import ma.naja7i.notification.infrastructure.persistence.NotificationRepository;
import ma.naja7i.notification.infrastructure.redis.RetryQueue;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Drains the per-channel Redis sorted sets every 10 seconds and re-invokes
 * {@link NotificationDispatcher#attempt(Notification)} for each due item.
 */
@Component
@Slf4j
public class RetryScheduler {

    private static final int BATCH = 50;

    private final RetryQueue retryQueue;
    private final NotificationDispatcher dispatcher;
    private final NotificationRepository repo;

    public RetryScheduler(RetryQueue retryQueue,
                          NotificationDispatcher dispatcher,
                          NotificationRepository repo) {
        this.retryQueue = retryQueue;
        this.dispatcher = dispatcher;
        this.repo = repo;
    }

    @Scheduled(fixedDelayString = "${naja7i.notification.retry.poll-interval-ms:10000}")
    public void drain() {
        for (Channel channel : Channel.values()) {
            Set<ZSetOperations.TypedTuple<String>> due = retryQueue.pollDue(channel, BATCH);
            if (due == null || due.isEmpty()) continue;
            for (ZSetOperations.TypedTuple<String> tuple : due) {
                String member = tuple.getValue();
                if (member == null) continue;
                try {
                    Long id = Long.parseLong(member);
                    Notification n = repo.findById(id).orElse(null);
                    if (n == null) {
                        retryQueue.remove(channel, member);
                        continue;
                    }
                    retryQueue.remove(channel, member);
                    dispatcher.attempt(n);
                } catch (NumberFormatException nfe) {
                    log.warn("Bad retry queue entry channel={} member={}", channel, member);
                    retryQueue.remove(channel, member);
                } catch (Exception ex) {
                    log.error("Retry drain failed for channel={} member={}: {}", channel, member, ex.toString());
                }
            }
        }
    }
}

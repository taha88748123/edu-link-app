package ma.naja7i.notification.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.notification.domain.Channel;
import ma.naja7i.notification.domain.Notification;
import ma.naja7i.notification.domain.NotificationStatus;
import ma.naja7i.notification.domain.SendOutcome;
import ma.naja7i.notification.infrastructure.channels.NotificationChannelSender;
import ma.naja7i.notification.infrastructure.messaging.KafkaPublisher;
import ma.naja7i.notification.infrastructure.persistence.NotificationRepository;
import ma.naja7i.notification.infrastructure.redis.RetryQueue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Central pipeline. Steps :
 * <ol>
 *   <li>Resolve channels (explicit override OR preference resolver).</li>
 *   <li>Render the template once per locale.</li>
 *   <li>Persist a {@link Notification} row in {@code QUEUED} status per channel.</li>
 *   <li>Invoke the matching {@link NotificationChannelSender}.</li>
 *   <li>On success → mark {@code SENT}, publish {@code naja7i.notification.delivered.v1}.</li>
 *   <li>On failure → schedule retry via {@link RetryQueue} OR mark {@code FAILED} when
 *       attempts exhausted and publish {@code naja7i.notification.failed.v1}.</li>
 * </ol>
 */
@Service
@Slf4j
public class NotificationDispatcher {

    private final TemplateRenderer renderer;
    private final PreferenceResolver preferences;
    private final NotificationRepository repo;
    private final RetryQueue retryQueue;
    private final KafkaPublisher kafka;
    private final ObjectMapper objectMapper;
    private final Map<Channel, NotificationChannelSender> senders;

    public NotificationDispatcher(
            TemplateRenderer renderer,
            PreferenceResolver preferences,
            NotificationRepository repo,
            RetryQueue retryQueue,
            KafkaPublisher kafka,
            ObjectMapper objectMapper,
            List<NotificationChannelSender> sendersList) {
        this.renderer = renderer;
        this.preferences = preferences;
        this.repo = repo;
        this.retryQueue = retryQueue;
        this.kafka = kafka;
        this.objectMapper = objectMapper;
        this.senders = new EnumMap<>(Channel.class);
        for (NotificationChannelSender s : sendersList) {
            this.senders.put(s.channel(), s);
        }
        log.info("Channel adapters bound: {}", this.senders.keySet());
    }

    public List<Notification> dispatch(DispatchRequest req) {
        List<Channel> targets = req.channels() != null && !req.channels().isEmpty()
                ? req.channels()
                : preferences.resolve(req.role());

        // Phase-1 routing : for OTP we want SMS or WA exclusively even if caller passed PUSH/EMAIL,
        // but the dispatcher is generic — callers (consumers) own that mapping.

        RenderedTemplate rendered = renderer.render(req.templateKey(), req.locale(), req.params());

        return targets.stream()
                .map(channel -> dispatchOne(req, channel, rendered))
                .toList();
    }

    private Notification dispatchOne(DispatchRequest req, Channel channel, RenderedTemplate rendered) {
        Notification notif = Notification.builder()
                .userId(req.userId())
                .channel(channel)
                .templateKey(req.templateKey())
                .locale(req.locale())
                .subject(rendered.subject())
                .body(rendered.body())
                .payload(serialize(req.params()))
                .status(NotificationStatus.QUEUED)
                .attempts((short) 0)
                .build();
        notif = repo.save(notif);
        return attempt(notif);
    }

    /**
     * Single-attempt path. Public so the {@link RetryScheduler} can re-invoke it.
     * Increments {@code attempts} every call (regardless of outcome).
     */
    @Transactional
    public Notification attempt(Notification notif) {
        notif.setAttempts((short) (notif.getAttempts() + 1));
        NotificationChannelSender sender = senders.get(notif.getChannel());
        if (sender == null) {
            log.error("No sender bound for channel {} — marking FAILED", notif.getChannel());
            notif.setStatus(NotificationStatus.FAILED);
            notif.setError("No adapter for channel " + notif.getChannel());
            Notification saved = repo.save(notif);
            kafka.publishFailed(saved.getId(), saved.getUserId(), saved.getChannel(), saved.getError());
            return saved;
        }

        SendOutcome outcome;
        try {
            outcome = sender.send(notif);
        } catch (Exception ex) {
            log.warn("Sender for {} threw : {}", notif.getChannel(), ex.toString());
            outcome = SendOutcome.failed(ex.getMessage());
        }

        if (outcome.success()) {
            notif.setStatus(NotificationStatus.SENT);
            notif.setSentAt(Instant.now());
            notif.setProviderMessageId(outcome.providerMessageId());
            notif.setError(null);
            Notification saved = repo.save(notif);
            kafka.publishDelivered(saved.getId(), saved.getUserId(), saved.getChannel(),
                    saved.getProviderMessageId());
            return saved;
        }

        notif.setError(outcome.error());
        boolean scheduled = retryQueue.schedule(notif.getId(), notif.getChannel(), notif.getAttempts());
        if (scheduled) {
            notif.setStatus(NotificationStatus.QUEUED);
            return repo.save(notif);
        }
        notif.setStatus(NotificationStatus.FAILED);
        Notification saved = repo.save(notif);
        kafka.publishFailed(saved.getId(), saved.getUserId(), saved.getChannel(), saved.getError());
        return saved;
    }

    private String serialize(Map<String, Object> params) {
        if (params == null) return "{}";
        try {
            return objectMapper.writeValueAsString(params);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}

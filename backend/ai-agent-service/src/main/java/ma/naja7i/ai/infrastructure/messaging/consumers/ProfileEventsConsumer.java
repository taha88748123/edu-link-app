package ma.naja7i.ai.infrastructure.messaging.consumers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ma.naja7i.ai.application.IdempotentEventGuard;
import ma.naja7i.ai.application.ProfileFeatureUpdater;
import ma.naja7i.ai.infrastructure.messaging.payload.ProfSubmittedConsumedPayload;
import ma.naja7i.ai.infrastructure.messaging.payload.ProfValidatedConsumedPayload;
import ma.naja7i.ai.infrastructure.messaging.payload.ReviewCreatedConsumedPayload;
import ma.naja7i.shared.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Hub for the three profile-related topics consumed by the AI service.
 *
 * <ul>
 *   <li>{@code prof.submitted}  -> dedup + log (feature recomputation is v2)</li>
 *   <li>{@code prof.validated}  -> dedup + upsert PROF feature row</li>
 *   <li>{@code review.created}  -> dedup + log (rating recomputation is v2)</li>
 * </ul>
 *
 * <p>Every handler calls {@link IdempotentEventGuard#markProcessed} as its
 * first step so duplicate Kafka deliveries are rejected before any side
 * effect. Acknowledgement always commits — re-delivery would just
 * re-trigger the dedup, wasting cycles.</p>
 */
@Component
@RequiredArgsConstructor
public class ProfileEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProfileEventsConsumer.class);

    private final ObjectMapper kafkaObjectMapper;
    private final IdempotentEventGuard guard;
    private final ProfileFeatureUpdater featureUpdater;

    @KafkaListener(
            topics = "${naja7i.kafka.topics.prof-submitted:naja7i.profile.prof.submitted.v1}",
            groupId = "${spring.kafka.consumer.group-id:ai-agent-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onProfSubmitted(Object message, Acknowledgment ack) {
        try {
            DomainEvent<ProfSubmittedConsumedPayload> event = kafkaObjectMapper.convertValue(
                    message, new TypeReference<>() {});
            ProfSubmittedConsumedPayload p = event.payload();
            if (p == null || p.userId() == null) {
                log.warn("prof.submitted missing userId — ignoring");
                return;
            }
            if (!guard.markProcessed(event.eventId(), event.eventType())) {
                return;
            }
            // v1: pure observability — feature recomputation lives in v2.
            log.debug("prof.submitted observed for {}", p.userId());
        } catch (Exception ex) {
            log.error("Failed to handle prof.submitted: {}", ex.getMessage(), ex);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = "${naja7i.kafka.topics.prof-validated:naja7i.profile.prof.validated.v1}",
            groupId = "${spring.kafka.consumer.group-id:ai-agent-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onProfValidated(Object message, Acknowledgment ack) {
        try {
            DomainEvent<ProfValidatedConsumedPayload> event = kafkaObjectMapper.convertValue(
                    message, new TypeReference<>() {});
            ProfValidatedConsumedPayload p = event.payload();
            if (p == null || p.userId() == null) {
                log.warn("prof.validated missing userId — ignoring");
                return;
            }
            if (!guard.markProcessed(event.eventId(), event.eventType())) {
                return;
            }
            featureUpdater.onValidated(p.userId());
        } catch (Exception ex) {
            log.error("Failed to handle prof.validated: {}", ex.getMessage(), ex);
        } finally {
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = "${naja7i.kafka.topics.review-created:naja7i.profile.review.created.v1}",
            groupId = "${spring.kafka.consumer.group-id:ai-agent-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onReviewCreated(Object message, Acknowledgment ack) {
        try {
            DomainEvent<ReviewCreatedConsumedPayload> event = kafkaObjectMapper.convertValue(
                    message, new TypeReference<>() {});
            ReviewCreatedConsumedPayload p = event.payload();
            if (p == null || p.reviewId() == null) {
                log.warn("review.created missing reviewId — ignoring");
                return;
            }
            if (!guard.markProcessed(event.eventId(), event.eventType())) {
                return;
            }
            // v1: log only — rating recomputation is a v2 concern.
            log.debug("review.created observed for profId={} stars={}", p.profId(), p.stars());
        } catch (Exception ex) {
            log.error("Failed to handle review.created: {}", ex.getMessage(), ex);
        } finally {
            ack.acknowledge();
        }
    }
}

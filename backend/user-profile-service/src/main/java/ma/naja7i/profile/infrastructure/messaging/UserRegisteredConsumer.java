package ma.naja7i.profile.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.naja7i.profile.application.ProfileService;
import ma.naja7i.profile.infrastructure.messaging.payload.UserRegisteredPayload;
import ma.naja7i.shared.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code naja7i.auth.user.registered.v1} and creates an empty profile.
 * Idempotent: relies on the underlying repository to return existing profile if the user_id
 * is already present.
 */
@Component
public class UserRegisteredConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredConsumer.class);

    private final ProfileService profileService;
    private final ObjectMapper mapper;

    public UserRegisteredConsumer(ProfileService profileService, ObjectMapper mapper) {
        this.profileService = profileService;
        this.mapper = mapper;
    }

    @KafkaListener(
            topics = "${naja7i.kafka.topics.auth-user-registered:naja7i.auth.user.registered.v1}",
            groupId = "${spring.kafka.consumer.group-id:user-profile-service}"
    )
    public void onUserRegistered(Object message) {
        try {
            DomainEvent<UserRegisteredPayload> event = mapper.convertValue(message,
                    mapper.getTypeFactory().constructParametricType(
                            DomainEvent.class, UserRegisteredPayload.class));
            UserRegisteredPayload payload = event.payload();
            if (payload == null || payload.userId() == null || payload.role() == null) {
                log.warn("Ignoring auth.user.registered event with missing fields: {}", message);
                return;
            }
            profileService.ensureProfileExists(payload.userId(), payload.role(), payload.locale());
            log.info("Profile ensured for userId={} role={}", payload.userId(), payload.role());
        } catch (Exception e) {
            log.error("Failed to handle auth.user.registered event: {}", e.getMessage(), e);
            // Swallow to let consumer continue; in v2 this should go to a DLT.
        }
    }
}

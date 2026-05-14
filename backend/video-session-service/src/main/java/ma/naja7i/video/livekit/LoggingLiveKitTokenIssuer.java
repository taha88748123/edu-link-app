package ma.naja7i.video.livekit;

import lombok.RequiredArgsConstructor;
import ma.naja7i.video.config.VideoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Stub token issuer for tests / non-LiveKit environments. Activated by
 * {@code naja7i.video.livekit.driver=logging}. The returned "token" is a
 * stable, non-cryptographic placeholder of the form
 * {@code dev-fake-token-<uuid>} — never embed it in a real LiveKit call.
 *
 * <p>We deliberately do NOT log the room/userId at INFO; this adapter exists
 * for hermetic tests, not for production observability.
 */
@Component
@ConditionalOnProperty(name = "naja7i.video.livekit.driver", havingValue = "logging")
@RequiredArgsConstructor
public class LoggingLiveKitTokenIssuer implements LiveKitTokenIssuer {

    private static final Logger log = LoggerFactory.getLogger(LoggingLiveKitTokenIssuer.class);

    public static final String STUB_PREFIX = "dev-fake-token-";

    private final VideoProperties properties;

    @Override
    public Token mint(UUID userId,
                      String roomName,
                      String roleLabel,
                      boolean canPublish,
                      boolean canSubscribe,
                      Instant expiresAt) {
        log.debug("[LIVEKIT:LOG] issuing stub token role={} room={}", roleLabel, roomName);
        String token = STUB_PREFIX + UUID.randomUUID();
        return new Token(token, properties.getLivekit().getUrl(), roomName, expiresAt);
    }
}

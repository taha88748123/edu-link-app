package ma.naja7i.video.livekit;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import ma.naja7i.video.config.VideoProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Real LiveKit-compatible token: HS256-signed JWT with the LiveKit
 * {@code video} grant claim. Selected by
 * {@code naja7i.video.livekit.driver=jwt} (default).
 *
 * <p>LiveKit (the SFU) verifies the token by reading {@code iss} (apiKey),
 * looking up the corresponding secret server-side and validating HS256. We
 * never log the produced token.
 */
@Component
@ConditionalOnProperty(name = "naja7i.video.livekit.driver", havingValue = "jwt", matchIfMissing = true)
@RequiredArgsConstructor
public class JjwtLiveKitTokenIssuer implements LiveKitTokenIssuer {

    private final VideoProperties properties;

    @Override
    public Token mint(UUID userId,
                      String roomName,
                      String roleLabel,
                      boolean canPublish,
                      boolean canSubscribe,
                      Instant expiresAt) {

        VideoProperties.LiveKit lk = properties.getLivekit();
        SecretKey key = Keys.hmacShaKeyFor(lk.getApiSecret().getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        // LiveKit "video" grant — see https://docs.livekit.io/realtime/concepts/authentication
        Map<String, Object> video = new LinkedHashMap<>();
        video.put("room", roomName);
        video.put("roomJoin", true);
        video.put("canPublish", canPublish);
        video.put("canSubscribe", canSubscribe);
        video.put("canPublishData", true);

        // Metadata MUST stay PII-free — only the role label.
        Map<String, Object> metadata = Map.of("role", roleLabel);

        String token = Jwts.builder()
                .issuer(lk.getApiKey())
                .subject(userId.toString())
                .notBefore(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("name", roleLabel)
                .claim("video", video)
                .claim("metadata", metadata)
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        return new Token(token, lk.getUrl(), roomName, expiresAt);
    }
}

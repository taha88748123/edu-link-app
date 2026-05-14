package ma.naja7i.video.livekit;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ma.naja7i.video.config.VideoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the two {@link LiveKitTokenIssuer} implementations. The JWT
 * issuer is exercised by signing and verifying with the same secret; the
 * logging issuer is checked for stub-prefix and PII absence.
 */
class LiveKitTokenIssuerTest {

    VideoProperties properties;
    UUID userId;

    @BeforeEach
    void setup() {
        properties = new VideoProperties();
        userId = UUID.randomUUID();
    }

    @Test
    void jjwt_producesParseableJwt_withRoomGrant() {
        JjwtLiveKitTokenIssuer issuer = new JjwtLiveKitTokenIssuer(properties);
        Instant expires = Instant.now().plus(Duration.ofMinutes(30));

        LiveKitTokenIssuer.Token token = issuer.mint(userId, "room-x", "STUDENT",
                true, true, expires);

        assertThat(token.token()).isNotBlank();
        assertThat(token.livekitUrl()).isEqualTo(properties.getLivekit().getUrl());
        assertThat(token.roomName()).isEqualTo("room-x");
        assertThat(token.expiresAt()).isEqualTo(expires);

        // Verify the JWT using the same secret.
        SecretKey key = Keys.hmacShaKeyFor(
                properties.getLivekit().getApiSecret().getBytes(StandardCharsets.UTF_8));
        Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token.token());
        Claims claims = jws.getPayload();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getIssuer()).isEqualTo(properties.getLivekit().getApiKey());
        assertThat(claims.get("video")).isNotNull();
        assertThat(claims.get("name")).isEqualTo("STUDENT");
    }

    @Test
    void logging_returnsStubToken_withStablePrefix() {
        LoggingLiveKitTokenIssuer issuer = new LoggingLiveKitTokenIssuer(properties);

        LiveKitTokenIssuer.Token token = issuer.mint(userId, "room-y", "PROF",
                true, true, Instant.now().plusSeconds(60));

        assertThat(token.token()).startsWith(LoggingLiveKitTokenIssuer.STUB_PREFIX);
        assertThat(token.roomName()).isEqualTo("room-y");
        assertThat(token.livekitUrl()).isEqualTo(properties.getLivekit().getUrl());
        // Stub token MUST NOT leak the userId.
        assertThat(token.token()).doesNotContain(userId.toString());
    }
}

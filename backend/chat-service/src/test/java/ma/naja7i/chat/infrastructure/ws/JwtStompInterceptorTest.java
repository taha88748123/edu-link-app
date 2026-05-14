package ma.naja7i.chat.infrastructure.ws;

import ma.naja7i.shared.dto.Role;
import ma.naja7i.shared.security.JwtClaims;
import ma.naja7i.shared.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtStompInterceptor}. Covers the four canonical paths:
 * <ol>
 *   <li>Valid token → principal attached.</li>
 *   <li>Missing Authorization header → reject.</li>
 *   <li>Token signed with the wrong secret → reject.</li>
 *   <li>Non-CONNECT frames → pass-through, no parsing.</li>
 *   <li>Token with a malformed bearer prefix → reject.</li>
 * </ol>
 */
class JwtStompInterceptorTest {

    private static final String SECRET = "test-secret-naja7i-stomp-interceptor-32+chars-ok";
    private static final String ISSUER = "naja7i-auth";
    private JwtService jwtService;
    private JwtStompInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, Duration.ofMinutes(15), ISSUER);
        interceptor = new JwtStompInterceptor(jwtService);
    }

    @Test
    void validToken_attachesPrincipalAndPasses() {
        UUID uid = UUID.randomUUID();
        String token = jwtService.issueAccessToken(uid, Role.STUDENT, "fr");

        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.CONNECT);
        acc.setNativeHeader(JwtClaims.HEADER_AUTHORIZATION, JwtClaims.BEARER_PREFIX + token);
        acc.setSessionAttributes(new HashMap<>());
        acc.setLeaveMutable(true);
        Message<byte[]> in = MessageBuilder.createMessage(new byte[0], acc.getMessageHeaders());

        Message<?> out = interceptor.preSend(in, null);

        assertThat(out).isNotNull();
        StompHeaderAccessor outAcc = StompHeaderAccessor.wrap(out);
        assertThat(outAcc.getUser())
                .as("principal must be attached to the returned message")
                .isInstanceOf(StompPrincipal.class);
        StompPrincipal p = (StompPrincipal) outAcc.getUser();
        assertThat(p.userId()).isEqualTo(uid);
        assertThat(p.role()).isEqualTo(Role.STUDENT);
        assertThat(p.getName()).isEqualTo(uid.toString());
    }

    @Test
    void missingAuthHeader_isRejected() {
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.CONNECT);
        acc.setLeaveMutable(true);
        Message<byte[]> in = MessageBuilder.createMessage(new byte[0], acc.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(in, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WS_AUTH_MISSING");
    }

    @Test
    void tokenSignedWithDifferentSecret_isRejected() {
        JwtService wrongIssuer = new JwtService("another-secret-32-chars-or-more-please!!", Duration.ofMinutes(15), ISSUER);
        String token = wrongIssuer.issueAccessToken(UUID.randomUUID(), Role.PROF, "en");

        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.CONNECT);
        acc.setNativeHeader(JwtClaims.HEADER_AUTHORIZATION, JwtClaims.BEARER_PREFIX + token);
        acc.setLeaveMutable(true);
        Message<byte[]> in = MessageBuilder.createMessage(new byte[0], acc.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(in, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WS_AUTH_INVALID");
    }

    @Test
    void nonConnectFrames_arePassedThrough_unchanged() {
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.SEND);
        acc.setDestination("/app/typing");
        acc.setLeaveMutable(true);
        Message<byte[]> in = MessageBuilder.createMessage(new byte[0], acc.getMessageHeaders());
        Message<?> out = interceptor.preSend(in, null);
        assertThat(out).isSameAs(in);
    }

    @Test
    void malformedBearerPrefix_isRejected() {
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.CONNECT);
        acc.setNativeHeader(JwtClaims.HEADER_AUTHORIZATION, "Token abc");
        acc.setLeaveMutable(true);
        Message<byte[]> in = MessageBuilder.createMessage(new byte[0], acc.getMessageHeaders());
        assertThatThrownBy(() -> interceptor.preSend(in, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WS_AUTH_MISSING");
    }
}

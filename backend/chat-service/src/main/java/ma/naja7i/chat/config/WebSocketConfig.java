package ma.naja7i.chat.config;

import lombok.RequiredArgsConstructor;
import ma.naja7i.chat.application.ConversationService;
import ma.naja7i.chat.infrastructure.ws.JwtStompInterceptor;
import ma.naja7i.chat.infrastructure.ws.SubscriptionAuthorizationInterceptor;
import ma.naja7i.shared.security.JwtService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Full STOMP-over-native-WebSocket configuration for the chat service.
 *
 * <h3>Endpoint</h3>
 * <ul>
 *   <li>Handshake URL: {@code /ws/chat} (no SockJS — modern browsers only).</li>
 *   <li>Auth: JWT in the {@code Authorization: Bearer …} STOMP header on CONNECT.</li>
 * </ul>
 *
 * <h3>Destinations</h3>
 * <ul>
 *   <li>{@code /topic/conversations/{id}/messages} — broadcast new messages (subscription
 *       guarded by {@link SubscriptionAuthorizationInterceptor}).</li>
 *   <li>{@code /user/queue/conversations/{id}/typing} — peer typing indicator.</li>
 *   <li>{@code /user/queue/presence} — own presence changes.</li>
 *   <li>{@code /app/**} — incoming client-to-server frames (typing pings, etc.).</li>
 * </ul>
 *
 * <p>The whole broker is {@code @ConditionalOnProperty} so we can switch it off
 * via {@code naja7i.chat.ws.enabled=false} (dev hot-reload, debug, tests).
 */
@Configuration
@EnableWebSocketMessageBroker
@ConditionalOnProperty(value = "naja7i.chat.ws.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final ConversationService conversationService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Simple in-memory broker — fine for v1; switch to RabbitMQ/Kafka relay later if needed.
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // No withSockJS() — we use native WebSockets only.
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                new JwtStompInterceptor(jwtService),
                new SubscriptionAuthorizationInterceptor(conversationService));
    }
}

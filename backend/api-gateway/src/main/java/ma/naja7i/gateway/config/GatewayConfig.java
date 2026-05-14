package ma.naja7i.gateway.config;

import ma.naja7i.shared.security.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class GatewayConfig {

    @Bean
    public JwtService jwtService(JwtProperties props) {
        return new JwtService(props.getSecret(), props.getAccessTtl(), props.getIssuer());
    }

    /**
     * CORS is applied via a {@link CorsWebFilter} bean — Spring Cloud Gateway is
     * reactive, so a plain {@code CorsConfigurationSource} bean is NOT picked up
     * by the gateway filter chain. The filter intercepts every request (including
     * the OPTIONS preflight) and emits the Access-Control-Allow-* headers.
     */
    @Bean
    public CorsWebFilter corsWebFilter(CorsProperties props) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(props.getAllowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setExposedHeaders(List.of("X-Trace-Id",
                "X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset",
                "Retry-After"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return new CorsWebFilter(source);
    }

    /**
     * Phase-0 routing : all microservices route via Eureka lb:// once they register.
     * Routes are defined here rather than in YAML so we keep a single source of truth.
     */
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Phase 1
                .route("auth-service", r -> r.path("/api/v1/auth/**")
                        .uri("lb://auth-service"))
                .route("user-profile-service", r -> r.path("/api/v1/profile/**", "/api/v1/profiles/**")
                        .uri("lb://user-profile-service"))
                .route("notification-service", r -> r.path("/api/v1/notifications/**")
                        .uri("lb://notification-service"))
                // Phase 2
                .route("calendar-service", r -> r.path("/api/v1/calendar/**", "/api/v1/slots/**")
                        .uri("lb://calendar-service"))
                .route("pricing-service", r -> r.path("/api/v1/pricing/**")
                        .uri("lb://pricing-service"))
                .route("booking-service", r -> r.path("/api/v1/bookings/**", "/api/v1/disputes/**")
                        .uri("lb://booking-service"))
                .route("payment-service", r -> r.path("/api/v1/payment/**", "/api/v1/topups/**", "/api/v1/payouts/**", "/api/v1/balances/**")
                        .uri("lb://payment-service"))
                // Phase 3
                .route("chat-service", r -> r.path("/api/v1/chat/**", "/api/v1/conversations/**")
                        .uri("lb://chat-service"))
                // WebSocket handshake for real-time chat — Spring Cloud Gateway routes the
                // HTTP/1.1 Upgrade transparently to the chat-service instance picked by Eureka.
                .route("chat-service-ws", r -> r.path("/ws/chat/**")
                        .uri("lb:ws://chat-service"))
                .route("video-session-service", r -> r.path("/api/v1/video/**", "/api/v1/sessions/**")
                        .uri("lb://video-session-service"))
                .route("ai-agent-service", r -> r.path("/api/v1/ai/**")
                        .uri("lb://ai-agent-service"))
                .build();
    }
}

package ma.naja7i.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.naja7i.gateway.config.PublicPaths;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.JwtClaims;
import ma.naja7i.shared.security.JwtService;
import ma.naja7i.shared.security.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Global filter that validates the JWT on every non-public route and propagates
 * the authenticated identity downstream via {@code X-User-Id} / {@code X-User-Role} headers.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final ObjectMapper mapper;

    public JwtAuthFilter(JwtService jwtService, ObjectMapper mapper) {
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String path = req.getURI().getPath();
        String traceId = req.getHeaders().getFirst(JwtClaims.HEADER_TRACE_ID);
        if (traceId == null) traceId = UUID.randomUUID().toString();

        if (PublicPaths.isPublic(path)) {
            return chain.filter(exchange.mutate().request(req.mutate()
                    .header(JwtClaims.HEADER_TRACE_ID, traceId)
                    .build()).build());
        }

        String authHeader = req.getHeaders().getFirst(JwtClaims.HEADER_AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(JwtClaims.BEARER_PREFIX)) {
            return reject(exchange, traceId, "AUTH_TOKEN_INVALID", "Missing or malformed Authorization header");
        }

        String token = authHeader.substring(JwtClaims.BEARER_PREFIX.length());
        try {
            UserContext ctx = jwtService.verify(token);
            ServerHttpRequest mutated = req.mutate()
                    .header(JwtClaims.HEADER_USER_ID, ctx.userId().toString())
                    .header(JwtClaims.HEADER_USER_ROLE, ctx.role().name())
                    .header(JwtClaims.HEADER_LOCALE, ctx.locale() == null ? "fr" : ctx.locale())
                    .header(JwtClaims.HEADER_TRACE_ID, traceId)
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            log.debug("JWT rejected: {}", e.getMessage());
            return reject(exchange, traceId, "AUTH_TOKEN_INVALID", "Invalid or expired token");
        }
    }

    private Mono<Void> reject(ServerWebExchange exchange, String traceId, String code, String message) {
        ServerHttpResponse resp = exchange.getResponse();
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ApiResponse.ApiError err = new ApiResponse.ApiError(
                code, message, "errors." + code.toLowerCase(), null, traceId);
        try {
            byte[] bytes = mapper.writeValueAsBytes(ApiResponse.error(err));
            DataBuffer buf = resp.bufferFactory().wrap(bytes);
            return resp.writeWith(Mono.just(buf));
        } catch (Exception e) {
            return resp.writeWith(Mono.just(resp.bufferFactory()
                    .wrap(("{\"error\":{\"code\":\"" + code + "\"}}").getBytes(StandardCharsets.UTF_8))));
        }
    }
}

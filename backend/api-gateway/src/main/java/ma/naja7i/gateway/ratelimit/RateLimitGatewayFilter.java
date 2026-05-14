package ma.naja7i.gateway.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.ConsumptionProbe;
import ma.naja7i.shared.dto.ApiResponse;
import ma.naja7i.shared.security.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Reactive {@link GlobalFilter} that enforces per-identity rate limits using
 * {@link BucketResolver}. Ordered <em>after</em> {@code JwtAuthFilter} so the
 * downstream {@code X-User-Id} / {@code X-User-Role} headers (populated by the
 * JWT filter) are visible.
 *
 * <p>On a successful consume:
 * <ul>
 *   <li>Adds {@code X-RateLimit-Limit}, {@code X-RateLimit-Remaining} and
 *       {@code X-RateLimit-Reset} response headers.</li>
 * </ul>
 *
 * <p>On exhaustion:
 * <ul>
 *   <li>Writes a JSON {@link ApiResponse} envelope with code
 *       {@code AUTH_RATE_LIMITED} and HTTP 429.</li>
 *   <li>Sets {@code Retry-After} (seconds) and {@code X-RateLimit-Remaining: 0}.</li>
 * </ul>
 */
@Component
@ConditionalOnBean(BucketResolver.class)
public class RateLimitGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitGatewayFilter.class);
    public static final String CODE_RATE_LIMITED = "AUTH_RATE_LIMITED";

    private final BucketResolver bucketResolver;
    private final ObjectMapper mapper;

    public RateLimitGatewayFilter(BucketResolver bucketResolver, ObjectMapper mapper) {
        this.bucketResolver = bucketResolver;
        this.mapper = mapper;
    }

    /** After JwtAuthFilter (-100), so X-User-* headers are populated. */
    @Override
    public int getOrder() {
        return -50;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String path = req.getURI().getPath();

        // Hard bypass — actuator / metrics MUST never be throttled (Prometheus scrapes them).
        if (path.startsWith(RedisBucketResolver.ACTUATOR_PREFIX)) {
            return chain.filter(exchange);
        }

        String userId = req.getHeaders().getFirst(JwtClaims.HEADER_USER_ID);
        String role = req.getHeaders().getFirst(JwtClaims.HEADER_USER_ROLE);
        String clientIp = extractClientIp(req);

        BucketResolver.RateLimitContext ctx = new BucketResolver.RateLimitContext(
                path, req.getMethod() == null ? "GET" : req.getMethod().name(),
                clientIp, userId, role);

        BucketResolver.Resolved resolved;
        try {
            resolved = bucketResolver.resolve(ctx);
        } catch (Exception e) {
            // Resolver failure (e.g. Redis blip) — fail open and log; never block traffic on infra hiccups.
            log.warn("Rate limit resolver failed (failing open): {}", e.getMessage());
            return chain.filter(exchange);
        }
        if (resolved == null) {
            return chain.filter(exchange);
        }

        ConsumptionProbe probe;
        try {
            probe = resolved.bucket().tryConsumeAndReturnRemaining(1);
        } catch (Exception e) {
            // Redis transient errors: fail open to preserve availability.
            log.warn("Rate limit consume failed for key={} (failing open): {}", resolved.key(), e.getMessage());
            return chain.filter(exchange);
        }

        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.add("X-RateLimit-Limit", String.valueOf(resolved.tier().getCapacity()));

        if (probe.isConsumed()) {
            headers.add("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            headers.add("X-RateLimit-Reset",
                    String.valueOf(Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds()));
            return chain.filter(exchange);
        }

        long retryAfterSec = Math.max(1L, Duration.ofNanos(probe.getNanosToWaitForReset()).toSeconds());
        return write429(exchange, retryAfterSec, resolved.tier().getCapacity());
    }

    private Mono<Void> write429(ServerWebExchange exchange, long retryAfterSec, long limit) {
        ServerHttpResponse resp = exchange.getResponse();
        resp.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        HttpHeaders headers = resp.getHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSec));
        headers.set("X-RateLimit-Limit", String.valueOf(limit));
        headers.set("X-RateLimit-Remaining", "0");
        headers.set("X-RateLimit-Reset", String.valueOf(retryAfterSec));

        String traceId = exchange.getRequest().getHeaders().getFirst(JwtClaims.HEADER_TRACE_ID);
        if (traceId == null) traceId = UUID.randomUUID().toString();
        ApiResponse.ApiError err = new ApiResponse.ApiError(
                CODE_RATE_LIMITED,
                "Too many requests, please retry in " + retryAfterSec + "s",
                "errors.auth_rate_limited",
                null,
                traceId);
        try {
            byte[] bytes = mapper.writeValueAsBytes(ApiResponse.error(err));
            DataBuffer buf = resp.bufferFactory().wrap(bytes);
            return resp.writeWith(Mono.just(buf));
        } catch (Exception e) {
            String fallback = "{\"error\":{\"code\":\"" + CODE_RATE_LIMITED + "\",\"retryAfterSec\":" + retryAfterSec + "}}";
            DataBuffer buf = resp.bufferFactory().wrap(fallback.getBytes(StandardCharsets.UTF_8));
            return resp.writeWith(Mono.just(buf));
        }
    }

    private static String extractClientIp(ServerHttpRequest req) {
        // Trust X-Forwarded-For when behind nginx/cloud LB; fall back to remote address.
        String xff = req.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        InetSocketAddress addr = req.getRemoteAddress();
        return addr == null || addr.getAddress() == null ? "unknown" : addr.getAddress().getHostAddress();
    }
}

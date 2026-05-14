package ma.naja7i.gateway.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.nio.charset.StandardCharsets;

/**
 * Default {@link BucketResolver} implementation backed by a Bucket4j
 * {@link ProxyManager} (Redis). The choice between tiers is intentionally
 * cheap (a couple of string matches) — it runs on every request.
 *
 * <p>Tier selection order:
 * <ol>
 *   <li>Actuator endpoints → {@code null} (never throttled).</li>
 *   <li>POST {@code /api/v1/auth/login}        → {@link RateLimitProperties#getLogin()} per IP.</li>
 *   <li>POST {@code /api/v1/auth/register}     → {@link RateLimitProperties#getRegister()} per IP.</li>
 *   <li>Authenticated request with role=ADMIN  → {@link RateLimitProperties#getAdmin()} per userId.</li>
 *   <li>Authenticated request (any other role) → {@link RateLimitProperties#getAuthenticated()} per userId.</li>
 *   <li>Anonymous request                       → {@link RateLimitProperties#getAnon()} per IP.</li>
 * </ol>
 */
public class RedisBucketResolver implements BucketResolver {

    private static final PathMatcher MATCHER = new AntPathMatcher();
    public static final String LOGIN_PATH = "/api/v1/auth/login";
    public static final String REGISTER_PATH = "/api/v1/auth/register";
    public static final String ACTUATOR_PREFIX = "/actuator/";

    private final ProxyManager<byte[]> proxyManager;
    private final RateLimitProperties props;

    public RedisBucketResolver(ProxyManager<byte[]> proxyManager, RateLimitProperties props) {
        this.proxyManager = proxyManager;
        this.props = props;
    }

    @Override
    public Resolved resolve(RateLimitContext ctx) {
        String path = ctx.path() == null ? "" : ctx.path();
        if (path.startsWith(ACTUATOR_PREFIX) || MATCHER.match("/actuator/**", path)) {
            return null;
        }
        boolean isPost = "POST".equalsIgnoreCase(ctx.method());

        // ---- auth-tightening tier (per IP, applies to POST only).
        if (isPost && LOGIN_PATH.equals(path)) {
            return build("rl:login:" + ipKey(ctx), props.getLogin());
        }
        if (isPost && REGISTER_PATH.equals(path)) {
            return build("rl:register:" + ipKey(ctx), props.getRegister());
        }

        // ---- per-user tiers.
        if (ctx.isAuthenticated()) {
            if ("ADMIN".equalsIgnoreCase(ctx.role())) {
                return build("rl:admin:" + ctx.userId(), props.getAdmin());
            }
            return build("rl:auth:" + ctx.userId(), props.getAuthenticated());
        }

        // ---- per-IP fallback.
        return build("rl:anon:" + ipKey(ctx), props.getAnon());
    }

    private Resolved build(String key, RateLimitProperties.Tier tier) {
        BucketConfiguration cfg = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(Math.max(1, tier.getCapacity()))
                        .refillGreedy(Math.max(1, tier.getRefillTokens()), tier.getRefillPeriod())
                        .build())
                .build();
        BucketProxy bucket = proxyManager.builder()
                .build(key.getBytes(StandardCharsets.UTF_8), () -> cfg);
        return new Resolved(bucket, tier, key);
    }

    private static String ipKey(RateLimitContext ctx) {
        return ctx.clientIp() == null || ctx.clientIp().isBlank() ? "unknown" : ctx.clientIp();
    }
}

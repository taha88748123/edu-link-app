package ma.naja7i.gateway.ratelimit;

import io.github.bucket4j.distributed.BucketProxy;

/**
 * Resolves the right Bucket4j bucket for the given request context.
 *
 * <p>Implementations are responsible for picking the tier (anon/auth/admin/login/register)
 * and building the bucket key (e.g. {@code "rl:auth:<userId>"} vs {@code "rl:anon:<ip>"}).
 */
public interface BucketResolver {

    /** A resolved bucket, plus the active tier (for X-RateLimit-* headers). */
    record Resolved(BucketProxy bucket, RateLimitProperties.Tier tier, String key) {}

    /** Returns {@code null} when the path is bypassed (actuator, etc.). */
    Resolved resolve(RateLimitContext ctx);

    record RateLimitContext(String path, String method, String clientIp, String userId, String role) {
        public boolean isAuthenticated() {
            return userId != null && !userId.isBlank();
        }
    }
}

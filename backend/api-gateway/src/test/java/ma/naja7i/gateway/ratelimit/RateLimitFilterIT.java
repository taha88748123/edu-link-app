package ma.naja7i.gateway.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the Redis-backed rate-limit pipeline.
 *
 * <p>Spins up a real Redis container via Testcontainers, builds the actual
 * Bucket4j proxy manager + resolver, and asserts the bucket behaviour:
 * <ol>
 *   <li>{@code login} tier (capacity=5) allows the first 5 hits.</li>
 *   <li>The 6th hit is exhausted and reports a positive retry-after.</li>
 *   <li>{@code anon} tier (capacity=30) gives plenty of headroom for 30 hits.</li>
 *   <li>Actuator paths bypass the limiter entirely (no bucket created).</li>
 * </ol>
 *
 * <p>If Docker is not available the test is skipped at the container start.
 */
@Testcontainers
class RateLimitFilterIT {

    private static GenericContainer<?> REDIS;
    private static RedisClient CLIENT;
    private static StatefulRedisConnection<byte[], byte[]> CONN;
    private static ProxyManager<byte[]> PROXY;

    @BeforeAll
    static void up() {
        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
        REDIS.start();
        RedisURI uri = RedisURI.builder()
                .withHost(REDIS.getHost())
                .withPort(REDIS.getMappedPort(6379))
                .withTimeout(Duration.ofSeconds(2))
                .build();
        CLIENT = RedisClient.create(uri);
        CONN = CLIENT.connect(ByteArrayCodec.INSTANCE);
        PROXY = LettuceBasedProxyManager.builderFor(CONN).build();
    }

    @AfterAll
    static void down() {
        if (CONN != null) CONN.close();
        if (CLIENT != null) CLIENT.shutdown();
        if (REDIS != null) REDIS.stop();
    }

    @Test
    void loginTier_allowsFirst5_thenRejects() {
        RateLimitProperties props = new RateLimitProperties();
        // Tighten further so the test is deterministic in <1s.
        props.setLogin(new RateLimitProperties.Tier(5, 5, Duration.ofMinutes(1)));

        // Unique key per test run — avoids state pollution between runs.
        String ip = "203.0.113." + (System.nanoTime() % 250);
        BucketResolver resolver = new RedisBucketResolver(PROXY, props);

        int success = 0;
        ConsumptionProbe lastProbe = null;
        for (int i = 0; i < 7; i++) {
            BucketResolver.RateLimitContext ctx = new BucketResolver.RateLimitContext(
                    RedisBucketResolver.LOGIN_PATH, "POST", ip, null, null);
            BucketResolver.Resolved r = resolver.resolve(ctx);
            assertThat(r).isNotNull();
            lastProbe = r.bucket().tryConsumeAndReturnRemaining(1);
            if (lastProbe.isConsumed()) success++;
        }
        assertThat(success).isEqualTo(5);
        assertThat(lastProbe).isNotNull();
        assertThat(lastProbe.isConsumed()).isFalse();
        assertThat(lastProbe.getNanosToWaitForReset()).isGreaterThan(0L);
    }

    @Test
    void anonTier_givesHeadroomFor30Hits() {
        RateLimitProperties props = new RateLimitProperties();
        String ip = "198.51.100." + (System.nanoTime() % 250);
        BucketResolver resolver = new RedisBucketResolver(PROXY, props);

        int success = 0;
        for (int i = 0; i < 30; i++) {
            BucketResolver.RateLimitContext ctx = new BucketResolver.RateLimitContext(
                    "/api/v1/profile/me", "GET", ip, null, null);
            BucketResolver.Resolved r = resolver.resolve(ctx);
            assertThat(r).isNotNull();
            if (r.bucket().tryConsumeAndReturnRemaining(1).isConsumed()) success++;
        }
        assertThat(success).isEqualTo(30);
    }

    @Test
    void actuatorPaths_bypassRateLimiter() {
        RateLimitProperties props = new RateLimitProperties();
        BucketResolver resolver = new RedisBucketResolver(PROXY, props);
        for (int i = 0; i < 100; i++) {
            BucketResolver.RateLimitContext ctx = new BucketResolver.RateLimitContext(
                    "/actuator/prometheus", "GET", "10.0.0.1", null, null);
            assertThat(resolver.resolve(ctx)).isNull();
        }
    }

    @Test
    void authenticatedAdminTier_hasMoreHeadroomThanRegularUser() {
        RateLimitProperties props = new RateLimitProperties();
        // Use a small admin capacity for a quick test.
        props.setAdmin(new RateLimitProperties.Tier(20, 20, Duration.ofMinutes(1)));
        props.setAuthenticated(new RateLimitProperties.Tier(10, 10, Duration.ofMinutes(1)));

        BucketResolver resolver = new RedisBucketResolver(PROXY, props);
        String adminId = "admin-" + System.nanoTime();
        String userId = "user-" + System.nanoTime();

        int adminSuccess = 0;
        for (int i = 0; i < 15; i++) {
            BucketResolver.RateLimitContext ctx = new BucketResolver.RateLimitContext(
                    "/api/v1/admin/foo", "GET", "127.0.0.1", adminId, "ADMIN");
            if (resolver.resolve(ctx).bucket().tryConsumeAndReturnRemaining(1).isConsumed()) {
                adminSuccess++;
            }
        }
        int userSuccess = 0;
        for (int i = 0; i < 15; i++) {
            BucketResolver.RateLimitContext ctx = new BucketResolver.RateLimitContext(
                    "/api/v1/profile/me", "GET", "127.0.0.1", userId, "STUDENT");
            if (resolver.resolve(ctx).bucket().tryConsumeAndReturnRemaining(1).isConsumed()) {
                userSuccess++;
            }
        }
        assertThat(adminSuccess).isEqualTo(15);
        assertThat(userSuccess).isEqualTo(10);
    }
}

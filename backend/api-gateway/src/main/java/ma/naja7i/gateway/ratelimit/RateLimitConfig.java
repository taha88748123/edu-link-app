package ma.naja7i.gateway.ratelimit;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Wires the Bucket4j {@link ProxyManager} backed by Lettuce/Redis. Used by
 * {@link RedisBucketResolver} to obtain a distributed bucket per identity.
 *
 * <p>When {@code naja7i.gateway.rate-limit.enabled=false} (e.g. local debug,
 * some CI scenarios) the rate limiter is disabled and no Redis connection is
 * opened. The {@code spring.data.redis.*} settings are reused so we keep a
 * single source of truth for the Redis URL.
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(value = "naja7i.gateway.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitConfig.class);

    @Bean(destroyMethod = "shutdown")
    public RedisClient rateLimitRedisClient(@Value("${spring.data.redis.host:localhost}") String host,
                                            @Value("${spring.data.redis.port:6379}") int port,
                                            @Value("${spring.data.redis.password:}") String password,
                                            @Value("${spring.data.redis.timeout:2s}") Duration timeout) {
        RedisURI.Builder uri = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withTimeout(timeout);
        if (password != null && !password.isBlank()) {
            uri.withPassword(password.toCharArray());
        }
        log.info("Initialising rate-limit RedisClient host={} port={}", host, port);
        return RedisClient.create(uri.build());
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<byte[], byte[]> rateLimitRedisConnection(RedisClient client) {
        return client.connect(ByteArrayCodec.INSTANCE);
    }

    @Bean
    @Primary
    public ProxyManager<byte[]> rateLimitProxyManager(StatefulRedisConnection<byte[], byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection).build();
    }

    @Bean
    public BucketResolver bucketResolver(ProxyManager<byte[]> proxyManager, RateLimitProperties props) {
        return new RedisBucketResolver(proxyManager, props);
    }
}

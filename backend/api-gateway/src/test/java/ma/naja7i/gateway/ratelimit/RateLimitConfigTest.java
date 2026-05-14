package ma.naja7i.gateway.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link RateLimitProperties} loads the 5 tiers correctly from
 * the property tree (binder-level check — no Spring context required).
 */
class RateLimitConfigTest {

    @Test
    void defaults_areApplied_whenNoOverrides() {
        RateLimitProperties props = bind(Map.of());

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getAnon().getCapacity()).isEqualTo(30);
        assertThat(props.getAnon().getRefillTokens()).isEqualTo(60);
        assertThat(props.getAnon().getRefillPeriod()).isEqualTo(Duration.ofMinutes(1));

        assertThat(props.getAuthenticated().getCapacity()).isEqualTo(150);
        assertThat(props.getAuthenticated().getRefillTokens()).isEqualTo(300);

        assertThat(props.getAdmin().getCapacity()).isEqualTo(300);
        assertThat(props.getAdmin().getRefillTokens()).isEqualTo(600);

        assertThat(props.getLogin().getCapacity()).isEqualTo(5);
        assertThat(props.getRegister().getCapacity()).isEqualTo(3);
    }

    @Test
    void allTiers_overrideable_fromProperties() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("naja7i.gateway.rate-limit.enabled", "false");
        map.put("naja7i.gateway.rate-limit.anon.capacity", "10");
        map.put("naja7i.gateway.rate-limit.anon.refill-tokens", "20");
        map.put("naja7i.gateway.rate-limit.anon.refill-period", "PT30S");
        map.put("naja7i.gateway.rate-limit.authenticated.capacity", "200");
        map.put("naja7i.gateway.rate-limit.admin.capacity", "999");
        map.put("naja7i.gateway.rate-limit.login.capacity", "2");
        map.put("naja7i.gateway.rate-limit.register.capacity", "1");

        RateLimitProperties props = bind(map);

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getAnon().getCapacity()).isEqualTo(10);
        assertThat(props.getAnon().getRefillTokens()).isEqualTo(20);
        assertThat(props.getAnon().getRefillPeriod()).isEqualTo(Duration.ofSeconds(30));
        assertThat(props.getAuthenticated().getCapacity()).isEqualTo(200);
        assertThat(props.getAdmin().getCapacity()).isEqualTo(999);
        assertThat(props.getLogin().getCapacity()).isEqualTo(2);
        assertThat(props.getRegister().getCapacity()).isEqualTo(1);
    }

    @Test
    void redisBucketResolver_returnsNull_forActuatorPaths() {
        // The actuator/Prometheus scrape path must NEVER be throttled.
        // We can verify the resolver's path-matching logic without a real Redis:
        RateLimitProperties props = new RateLimitProperties();
        // Pass a null ProxyManager — we never reach .build() for actuator paths.
        RedisBucketResolver resolver = new RedisBucketResolver(null, props);
        BucketResolver.RateLimitContext ctx = new BucketResolver.RateLimitContext(
                "/actuator/prometheus", "GET", "127.0.0.1", null, null);
        assertThat(resolver.resolve(ctx)).isNull();
    }

    private static RateLimitProperties bind(Map<String, Object> map) {
        ConfigurationPropertySource src = new MapConfigurationPropertySource(map);
        return new Binder(src)
                .bind("naja7i.gateway.rate-limit", RateLimitProperties.class)
                .orElseGet(RateLimitProperties::new);
    }
}

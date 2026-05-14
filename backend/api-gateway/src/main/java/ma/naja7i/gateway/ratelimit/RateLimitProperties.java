package ma.naja7i.gateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Tiered rate-limit configuration. All counts are expressed in tokens.
 *
 * <p>The bucket model is "token-bucket-per-identity":
 * <ul>
 *   <li>{@code capacity} is the burst size (immediately available).</li>
 *   <li>{@code refillTokens} is the steady-state refill within {@code refillPeriod}.</li>
 * </ul>
 *
 * <p>Tiers:
 * <ul>
 *   <li><b>anon</b>: per client IP, for all non-authenticated requests.</li>
 *   <li><b>authenticated</b>: per userId, for regular users (STUDENT/PROF/PARENT).</li>
 *   <li><b>admin</b>: per userId, much higher budget for ADMIN.</li>
 *   <li><b>login</b>: per IP, tight cap on POST {@code /api/v1/auth/login}.</li>
 *   <li><b>register</b>: per IP, even tighter on POST {@code /api/v1/auth/register}.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "naja7i.gateway.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private Tier anon = new Tier(30, 60, Duration.ofMinutes(1));
    private Tier authenticated = new Tier(150, 300, Duration.ofMinutes(1));
    private Tier admin = new Tier(300, 600, Duration.ofMinutes(1));
    private Tier login = new Tier(5, 5, Duration.ofMinutes(1));
    private Tier register = new Tier(3, 3, Duration.ofMinutes(1));

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Tier getAnon() { return anon; }
    public void setAnon(Tier anon) { this.anon = anon; }
    public Tier getAuthenticated() { return authenticated; }
    public void setAuthenticated(Tier authenticated) { this.authenticated = authenticated; }
    public Tier getAdmin() { return admin; }
    public void setAdmin(Tier admin) { this.admin = admin; }
    public Tier getLogin() { return login; }
    public void setLogin(Tier login) { this.login = login; }
    public Tier getRegister() { return register; }
    public void setRegister(Tier register) { this.register = register; }

    public static class Tier {
        private long capacity;
        private long refillTokens;
        private Duration refillPeriod = Duration.ofMinutes(1);

        public Tier() {}

        public Tier(long capacity, long refillTokens, Duration refillPeriod) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillPeriod = refillPeriod;
        }

        public long getCapacity() { return capacity; }
        public void setCapacity(long capacity) { this.capacity = capacity; }
        public long getRefillTokens() { return refillTokens; }
        public void setRefillTokens(long refillTokens) { this.refillTokens = refillTokens; }
        public Duration getRefillPeriod() { return refillPeriod; }
        public void setRefillPeriod(Duration refillPeriod) { this.refillPeriod = refillPeriod; }
    }
}

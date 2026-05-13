package ma.naja7i.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT settings bound to {@code naja7i.jwt.*}. The {@code secret} value MUST be identical
 * to the one configured in api-gateway (env {@code NAJA7I_JWT_SECRET}).
 */
@ConfigurationProperties(prefix = "naja7i.jwt")
public class JwtProperties {

    private String secret = "dev-only-secret-change-me-32+chars-required";
    private String issuer = "naja7i-auth";
    private Duration accessTtl = Duration.ofMinutes(15);
    private Duration refreshTtl = Duration.ofDays(14);

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public Duration getAccessTtl() { return accessTtl; }
    public void setAccessTtl(Duration accessTtl) { this.accessTtl = accessTtl; }

    public Duration getRefreshTtl() { return refreshTtl; }
    public void setRefreshTtl(Duration refreshTtl) { this.refreshTtl = refreshTtl; }
}

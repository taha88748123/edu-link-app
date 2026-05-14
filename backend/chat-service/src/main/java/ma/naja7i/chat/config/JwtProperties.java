package ma.naja7i.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT settings bound to {@code naja7i.jwt.*}. The {@code secret} value MUST be
 * identical to the one configured in api-gateway and auth-service.
 *
 * <p>Used by the STOMP {@code CONNECT} interceptor to validate the access token
 * embedded in the {@code Authorization: Bearer …} STOMP header.
 */
@ConfigurationProperties(prefix = "naja7i.jwt")
public class JwtProperties {

    private String secret = "dev-only-secret-change-me-32+chars-required";
    private String issuer = "naja7i-auth";
    private Duration accessTtl = Duration.ofMinutes(15);

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public Duration getAccessTtl() { return accessTtl; }
    public void setAccessTtl(Duration accessTtl) { this.accessTtl = accessTtl; }
}

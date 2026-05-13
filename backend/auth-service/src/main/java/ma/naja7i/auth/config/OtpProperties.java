package ma.naja7i.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "naja7i.otp")
public class OtpProperties {

    private Duration ttl = Duration.ofMinutes(5);
    private int maxAttempts = 5;
    private int rateLimitWindowMinutes = 15;
    private int rateLimitMaxRequests = 3;
    private int codeLength = 6;

    public Duration getTtl() { return ttl; }
    public void setTtl(Duration ttl) { this.ttl = ttl; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public int getRateLimitWindowMinutes() { return rateLimitWindowMinutes; }
    public void setRateLimitWindowMinutes(int rateLimitWindowMinutes) { this.rateLimitWindowMinutes = rateLimitWindowMinutes; }

    public int getRateLimitMaxRequests() { return rateLimitMaxRequests; }
    public void setRateLimitMaxRequests(int rateLimitMaxRequests) { this.rateLimitMaxRequests = rateLimitMaxRequests; }

    public int getCodeLength() { return codeLength; }
    public void setCodeLength(int codeLength) { this.codeLength = codeLength; }
}

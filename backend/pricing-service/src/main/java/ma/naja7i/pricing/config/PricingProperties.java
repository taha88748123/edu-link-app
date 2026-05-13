package ma.naja7i.pricing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Externalised configuration for pricing-service. Bound via {@code naja7i.pricing.*}
 * in {@code application.yml}.
 */
@ConfigurationProperties(prefix = "naja7i.pricing")
public class PricingProperties {

    /** Fallback platform fee percentage when no row with {@code name='STANDARD'} exists. */
    private BigDecimal defaultPlatformFeePct = new BigDecimal("10.0");

    /** Shared secret required on {@code /api/v1/internal/**} routes. */
    private String internalKey = "dev-internal-key";

    public BigDecimal getDefaultPlatformFeePct() {
        return defaultPlatformFeePct;
    }

    public void setDefaultPlatformFeePct(BigDecimal defaultPlatformFeePct) {
        this.defaultPlatformFeePct = defaultPlatformFeePct;
    }

    public String getInternalKey() {
        return internalKey;
    }

    public void setInternalKey(String internalKey) {
        this.internalKey = internalKey;
    }
}

package ma.naja7i.booking.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Service-level configuration read from {@code naja7i.booking.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "naja7i.booking")
public class BookingProperties {

    /** Shared secret used when calling other services on their {@code /api/v1/internal/**} endpoints. */
    private String internalKey = "dev-internal-key";

    /** Dispute window (in hours) after CONFIRMEE during which a student may still raise a dispute. */
    private int disputeWindowHours = 24;

    /** Tolerance (in minutes) before {@code scheduled_start} during which a PROF may already :start a session. */
    private int leadToleranceMinutes = 15;

    /** Outbox poller cadence (milliseconds). */
    private long outboxPollMs = 2000L;

    /** Max rows fetched per outbox poll. */
    private int outboxBatchSize = 50;

    /** REST clients connect timeout, in milliseconds. */
    private int restConnectTimeoutMs = 3000;

    /** REST clients read timeout, in milliseconds. */
    private int restReadTimeoutMs = 5000;
}

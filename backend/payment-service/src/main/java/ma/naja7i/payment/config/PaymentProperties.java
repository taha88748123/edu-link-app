package ma.naja7i.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Service-level configuration read from {@code naja7i.payment.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "naja7i.payment")
public class PaymentProperties {

    /** Outbox poller cadence (milliseconds). */
    private long outboxPollMs = 2000L;

    /** Max rows fetched per outbox poll. */
    private int outboxBatchSize = 50;

    /** Dispute window in hours (informational; the booking-service is the source of truth). */
    private int disputeWindowHours = 24;

    /**
     * Platform fee in basis points (1 bps = 0.01%). Used as a fallback when a
     * {@code booking.requested} event arrives without a pre-computed fee. The
     * fee is taken on the locked amount: {@code fee = amount * bps / 10_000}.
     */
    private int platformFeeBps = 1_000;
}

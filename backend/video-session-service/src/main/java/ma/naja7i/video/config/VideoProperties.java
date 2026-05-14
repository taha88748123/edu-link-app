package ma.naja7i.video.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Service-level configuration read from {@code naja7i.video.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "naja7i.video")
public class VideoProperties {

    /** Outbox poller cadence (milliseconds). */
    private long outboxPollMs = 2000L;

    /** Max rows fetched per outbox poll. */
    private int outboxBatchSize = 50;

    /** Default session duration when a booking event omits scheduled times. */
    private int defaultDurationMinutes = 60;

    /** LiveKit token / wiring settings. */
    private LiveKit livekit = new LiveKit();

    @Getter
    @Setter
    public static class LiveKit {
        /** {@code jwt} mints a real LiveKit-compatible JWT; {@code logging} returns a fake stub. */
        private String driver = "jwt";

        /** Front-end facing LiveKit URL. Frontend reads this from the token response. */
        private String url = "wss://livekit.localhost:7880";

        /** LiveKit API key (used as JWT issuer claim). */
        private String apiKey = "dev-api-key";

        /**
         * LiveKit API secret used to sign the JWT (HS256). MUST be at least 32 chars
         * for HS256 in jjwt 0.12+. Override in production.
         */
        private String apiSecret = "dev-api-secret-32-chars-minimum-padding";

        /** Extra TTL (minutes) on top of {@code scheduled_end} when issuing tokens. */
        private int tokenTtlBufferMinutes = 30;

        /** How early before {@code scheduled_start} a participant may join. */
        private int preliveBufferMinutes = 5;
    }
}

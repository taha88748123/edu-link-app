package ma.naja7i.ai.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Root configuration for the AI agent service, bound under
 * {@code naja7i.ai.*}. All driver defaults are {@code logging} so the
 * service starts with no external credentials.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "naja7i.ai")
public class AiProperties {

    /** Stable producer name embedded in outbound {@code DomainEvent.producer}. */
    private String producer = "ai-agent-service";

    /** Outbox poller cadence (milliseconds). */
    private long outboxPollMs = 2000L;

    /** Max outbox rows fetched per poll tick. */
    private int outboxBatchSize = 50;

    private Matching matching = new Matching();
    private Anthropic anthropic = new Anthropic();

    @Getter
    @Setter
    public static class Matching {
        /** {@code logging} (default, deterministic local scoring) or {@code anthropic}. */
        private String driver = "logging";

        /** Redis TTL applied to cached suggestion lists. */
        private int cacheTtlMinutes = 60;

        /** Upper bound on the number of suggestions returned to the caller. */
        private int maxResults = 20;
    }

    /**
     * Anthropic API tunables. The api-key is treated as a secret: it is
     * never logged or included in {@code toString()}.
     */
    @Getter
    @Setter
    @ToString
    public static class Anthropic {
        @ToString.Exclude
        private String apiKey = "";

        private String model = "claude-opus-4-7";

        private int maxTokens = 1024;

        private int timeoutMs = 8000;

        private String baseUrl = "https://api.anthropic.com";
    }
}

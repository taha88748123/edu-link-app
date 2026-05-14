package ma.naja7i.chat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Service-level configuration read from {@code naja7i.chat.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "naja7i.chat")
public class ChatProperties {

    /** Shared secret protecting {@code /api/v1/internal/**} routes. */
    private String internalKey = "dev-internal-key";

    /** Service identifier emitted in {@code DomainEvent.producer}. */
    private String producer = "chat-service";

    private final Presence presence = new Presence();
    private final Typing typing = new Typing();
    private final Moderation moderation = new Moderation();
    private final Bypass bypass = new Bypass();
    private final Anthropic anthropic = new Anthropic();

    @Getter @Setter
    public static class Presence {
        /** TTL (seconds) of the {@code chat:presence:{userId}} key. */
        private int ttlSeconds = 30;
    }

    @Getter @Setter
    public static class Typing {
        /** TTL (seconds) of the {@code chat:typing:{conversationId}} entries. */
        private int ttlSeconds = 5;
    }

    @Getter @Setter
    public static class Moderation {
        /** {@code logging} (default, no API call) or {@code anthropic} (Claude analysis). */
        private String driver = "logging";
    }

    @Getter @Setter
    public static class Bypass {
        /** Number of HIGH-severity hits in the rolling window before we escalate to CRITICAL. */
        private int strikeThreshold = 3;

        /** Width of the rolling window (hours) for the bypass strike tracker. */
        private int strikeWindowHours = 24;
    }

    @Getter @Setter
    public static class Anthropic {
        private String baseUrl = "https://api.anthropic.com";
        private String apiKey  = "";
        private String model   = "claude-sonnet-4-5";
        private int timeoutMs  = 5000;
        /** Optional override of the system prompt used for bypass-intent detection. */
        private String systemPrompt =
                "You are a fraud detector for an EdTech platform. Determine if the message "
              + "intends to share contact info or move the conversation off-platform. "
              + "Return strict JSON: {\"flags\":[\"...\"],\"severity\":\"LOW|MEDIUM|HIGH\"}.";
    }
}

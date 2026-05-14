package ma.naja7i.chat.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default async analyser — performs no external call. Active when
 * {@code naja7i.chat.moderation.driver} is {@code logging} (the default).
 *
 * <p>Note: never log the {@code originalBody} content (PII leak). We only emit the
 * IDs needed to correlate with a future investigation.</p>
 */
@Component
@ConditionalOnProperty(name = "naja7i.chat.moderation.driver", havingValue = "logging", matchIfMissing = true)
@Slf4j
public class LoggingBypassAnalyzer implements BypassAnalyzer {

    @Override
    @Async
    public void analyse(UUID messageId, UUID conversationId, UUID senderId, String originalBody) {
        log.info("[bypass:LOG] messageId={} conversationId={} bodyLen={} — driver=logging, no AI call",
                messageId, conversationId,
                originalBody == null ? 0 : originalBody.length());
    }
}

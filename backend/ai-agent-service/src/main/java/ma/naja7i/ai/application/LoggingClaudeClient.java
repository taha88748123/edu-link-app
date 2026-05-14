package ma.naja7i.ai.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default driver — always returns {@code null}. The matching pipeline reads
 * this as "skip LLM" and falls back to its deterministic baseline scorer.
 * Active when {@code naja7i.ai.matching.driver=logging} (the default).
 */
@Component
@ConditionalOnProperty(name = "naja7i.ai.matching.driver",
        havingValue = "logging", matchIfMissing = true)
public class LoggingClaudeClient implements ClaudeClient {

    private static final Logger log = LoggerFactory.getLogger(LoggingClaudeClient.class);

    @Override
    public Result messages(String prompt, UUID traceId) {
        log.debug("[LOG-DRIVER] Skipping Claude call (traceId={}, promptLength={})",
                traceId, prompt == null ? 0 : prompt.length());
        return null;
    }
}

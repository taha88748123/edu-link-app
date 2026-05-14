package ma.naja7i.ai.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.ai.config.AiProperties;
import ma.naja7i.ai.infrastructure.http.AnthropicHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Real Anthropic driver. Active when
 * {@code naja7i.ai.matching.driver=anthropic}. On any failure
 * (4xx / 5xx / missing creds / timeout) we log a warning with the
 * traceId and return {@code null}, signalling the matching pipeline to
 * fall back to its baseline scorer.
 */
@Component
@ConditionalOnProperty(name = "naja7i.ai.matching.driver", havingValue = "anthropic")
@RequiredArgsConstructor
public class AnthropicClaudeClient implements ClaudeClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClaudeClient.class);

    private final AnthropicHttpClient httpClient;
    private final AiProperties properties;

    @Override
    public Result messages(String prompt, UUID traceId) {
        long t0 = System.currentTimeMillis();
        try {
            Map<String, Object> response = httpClient.postMessages(prompt);
            if (response == null) {
                log.warn("Anthropic returned an empty body (traceId={})", traceId);
                return null;
            }
            String content = extractText(response);
            int tokensIn = extractUsage(response, "input_tokens");
            int tokensOut = extractUsage(response, "output_tokens");
            int latency = (int) (System.currentTimeMillis() - t0);
            return new Result(properties.getAnthropic().getModel(),
                    content == null ? "" : content,
                    tokensIn, tokensOut, latency);
        } catch (Exception ex) {
            log.warn("Anthropic call failed (traceId={}): {}", traceId, ex.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractText(Map<String, Object> response) {
        Object content = response.get("content");
        if (!(content instanceof List<?> blocks)) return null;
        for (Object block : blocks) {
            if (block instanceof Map<?, ?> b
                    && "text".equals(b.get("type"))
                    && b.get("text") instanceof String text) {
                return text;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static int extractUsage(Map<String, Object> response, String key) {
        Object usage = response.get("usage");
        if (usage instanceof Map<?, ?> u && u.get(key) instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }
}

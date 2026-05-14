package ma.naja7i.ai.infrastructure.http;

import lombok.RequiredArgsConstructor;
import ma.naja7i.ai.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Thin {@link RestClient} wrapper around the Anthropic messages endpoint.
 *
 * <p>Sensitive values (api-key, request body, response body) are never
 * logged. Failures are caught upstream by {@code AnthropicClaudeClient}
 * which converts them into a {@code null} result so the matching pipeline
 * silently falls back to its baseline scorer.</p>
 */
@Component
@ConditionalOnProperty(name = "naja7i.ai.matching.driver", havingValue = "anthropic")
@RequiredArgsConstructor
public class AnthropicHttpClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicHttpClient.class);
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final AiProperties properties;
    private RestClient client;

    private RestClient client() {
        if (client == null) {
            AiProperties.Anthropic cfg = properties.getAnthropic();
            client = RestClient.builder()
                    .baseUrl(cfg.getBaseUrl())
                    .requestFactory(buildRequestFactory(cfg.getTimeoutMs()))
                    .build();
        }
        return client;
    }

    /**
     * POST /v1/messages with the canonical Anthropic body shape. Returns
     * the decoded JSON map (never the raw string). Caller is responsible
     * for catching {@link RuntimeException} and downgrading to null.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> postMessages(String prompt) {
        AiProperties.Anthropic cfg = properties.getAnthropic();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new IllegalStateException("anthropic api-key is missing");
        }
        Map<String, Object> body = Map.of(
                "model", cfg.getModel(),
                "max_tokens", cfg.getMaxTokens(),
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );
        return client().post()
                .uri("/v1/messages")
                .header("x-api-key", cfg.getApiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .retrieve()
                .body(Map.class);
    }

    private org.springframework.http.client.ClientHttpRequestFactory buildRequestFactory(int timeoutMs) {
        var settings = org.springframework.boot.web.client.ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(timeoutMs))
                .withReadTimeout(Duration.ofMillis(timeoutMs));
        return org.springframework.boot.web.client.ClientHttpRequestFactories.get(settings);
    }
}

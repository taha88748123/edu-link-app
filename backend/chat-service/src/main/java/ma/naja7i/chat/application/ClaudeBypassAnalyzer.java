package ma.naja7i.chat.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ma.naja7i.chat.config.ChatProperties;
import ma.naja7i.chat.domain.Message;
import ma.naja7i.chat.domain.Moderation;
import ma.naja7i.chat.domain.ModerationStatus;
import ma.naja7i.chat.infrastructure.messaging.KafkaEventPublisher;
import ma.naja7i.chat.infrastructure.messaging.payload.ChatBypassFlaggedPayload;
import ma.naja7i.chat.infrastructure.persistence.MessageRepository;
import ma.naja7i.shared.events.EventTopics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Anthropic-driven bypass-intent analyser. Active when
 * {@code naja7i.chat.moderation.driver=anthropic} AND a non-empty API key is configured.
 * Otherwise the bean refuses to start and the {@link LoggingBypassAnalyzer} takes over.
 *
 * <p>The call is fire-and-forget ({@code @Async}). Any failure (timeout / 4xx / 5xx /
 * malformed response) is caught and downgraded to a log line — chat NEVER blocks on
 * the upstream AI.</p>
 */
@Component
@ConditionalOnProperty(name = "naja7i.chat.moderation.driver", havingValue = "anthropic")
@Slf4j
public class ClaudeBypassAnalyzer implements BypassAnalyzer {

    private final ChatProperties props;
    private final RestClient http;
    private final ObjectMapper mapper;
    private final MessageRepository messageRepository;
    private final KafkaEventPublisher kafkaPublisher;
    private final BypassStrikeTracker strikeTracker;

    public ClaudeBypassAnalyzer(ChatProperties props,
                                RestClient anthropicRestClient,
                                ObjectMapper objectMapper,
                                MessageRepository messageRepository,
                                KafkaEventPublisher kafkaPublisher,
                                BypassStrikeTracker strikeTracker) {
        this.props = props;
        this.http = anthropicRestClient;
        this.mapper = objectMapper;
        this.messageRepository = messageRepository;
        this.kafkaPublisher = kafkaPublisher;
        this.strikeTracker = strikeTracker;
    }

    @Override
    @Async
    public void analyse(UUID messageId, UUID conversationId, UUID senderId, String originalBody) {
        if (originalBody == null || originalBody.isBlank()) return;
        if (props.getAnthropic().getApiKey() == null || props.getAnthropic().getApiKey().isBlank()) {
            log.warn("anthropic driver active but no API key — skipping (messageId={})", messageId);
            return;
        }

        AiVerdict verdict;
        try {
            verdict = callAnthropic(originalBody);
        } catch (Exception ex) {
            log.warn("Anthropic call failed (will not block): messageId={} cause={}",
                    messageId, ex.getMessage());
            return;
        }
        if (verdict == null || verdict.flags.isEmpty()) return;

        Optional<Message> opt = messageRepository.findById(messageId);
        if (opt.isEmpty()) return;
        Message msg = opt.get();

        Moderation mod = msg.getModeration() == null ? Moderation.clean() : msg.getModeration();
        Set<String> merged = new LinkedHashSet<>(mod.getFlags() == null ? List.of() : mod.getFlags());
        merged.addAll(verdict.flags);
        mod.setFlags(new ArrayList<>(merged));
        if (mod.getStatus() == null || mod.getStatus() == ModerationStatus.CLEAN) {
            mod.setStatus(ModerationStatus.FLAGGED);
        }
        msg.setModeration(mod);
        messageRepository.save(msg);

        // Emit a fresh chat.bypass.flagged.v1 with the updated payload.
        kafkaPublisher.publish(
                EventTopics.CHAT_MESSAGE_POSTED.replace("message.posted", "bypass.flagged"),
                conversationId.toString(),
                "naja7i.chat.bypass.flagged.v1",
                new ChatBypassFlaggedPayload(messageId, conversationId, senderId,
                        new ArrayList<>(merged), verdict.severity));

        // HIGH severity bumps the strike tracker.
        if ("HIGH".equalsIgnoreCase(verdict.severity)) {
            BypassStrikeTracker.StrikeOutcome out = strikeTracker.recordStrike(senderId);
            if (out.shouldEscalate()) {
                kafkaPublisher.publish(
                        "naja7i.chat.bypass.flagged.v1",
                        conversationId.toString(),
                        "naja7i.chat.bypass.flagged.v1",
                        new ChatBypassFlaggedPayload(messageId, conversationId, senderId,
                                List.of("STRIKE_THRESHOLD_EXCEEDED", "SUGGEST_SUSPEND"),
                                "CRITICAL"));
            }
        }
    }

    private AiVerdict callAnthropic(String body) throws Exception {
        Map<String, Object> request = Map.of(
                "model", props.getAnthropic().getModel(),
                "max_tokens", 256,
                "system", props.getAnthropic().getSystemPrompt(),
                "messages", List.of(Map.of("role", "user", "content", body))
        );

        String responseJson = http.post()
                .uri("/v1/messages")
                .header("x-api-key", props.getAnthropic().getApiKey())
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

        if (responseJson == null) return null;
        JsonNode root = mapper.readTree(responseJson);
        JsonNode content = root.path("content");
        if (!content.isArray() || content.isEmpty()) return null;
        String text = content.get(0).path("text").asText("");
        if (text.isBlank()) return null;

        // Find the JSON object in the response (the model is told to return strict JSON,
        // but be defensive against leading prose).
        int lbrace = text.indexOf('{');
        int rbrace = text.lastIndexOf('}');
        if (lbrace < 0 || rbrace <= lbrace) return null;
        JsonNode verdict = mapper.readTree(text.substring(lbrace, rbrace + 1));

        List<String> flags = new ArrayList<>();
        verdict.path("flags").forEach(n -> flags.add(n.asText()));
        String severity = verdict.path("severity").asText("LOW");
        return new AiVerdict(flags, severity);
    }

    private record AiVerdict(List<String> flags, String severity) {}
}

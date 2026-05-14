package ma.naja7i.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.naja7i.ai.config.AiProperties;
import ma.naja7i.ai.domain.AiAgent;
import ma.naja7i.ai.domain.MatchingFeatures;
import ma.naja7i.ai.domain.OutboxEvent;
import ma.naja7i.ai.infrastructure.persistence.MatchingFeaturesRepository;
import ma.naja7i.ai.infrastructure.persistence.OutboxRepository;
import ma.naja7i.ai.infrastructure.redis.MatchingCacheStore;
import ma.naja7i.ai.infrastructure.web.dto.MatchSuggestion;
import ma.naja7i.ai.infrastructure.web.dto.MatchingRequest;
import ma.naja7i.ai.infrastructure.web.dto.MatchingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock MatchingFeaturesRepository matchingFeaturesRepository;
    @Mock ClaudeClient claudeClient;
    @Mock AiCallLogger aiCallLogger;
    @Mock MatchingCacheStore cacheStore;
    @Mock OutboxRepository outboxRepository;
    @Mock KafkaEventEnvelopeBuilder envelopeBuilder;

    MatchingService service;
    MatchScoreCalculator scoreCalculator;
    AiProperties properties;
    ObjectMapper mapper;

    UUID studentId;

    @BeforeEach
    void setup() {
        mapper = new ObjectMapper();
        scoreCalculator = new MatchScoreCalculator(mapper);
        properties = new AiProperties();
        // Default driver = "logging" already; explicit for clarity:
        properties.getMatching().setDriver("logging");
        properties.getMatching().setMaxResults(20);
        properties.getMatching().setCacheTtlMinutes(60);
        service = new MatchingService(matchingFeaturesRepository, scoreCalculator,
                claudeClient, aiCallLogger, cacheStore, outboxRepository,
                envelopeBuilder, properties, mapper);
        studentId = UUID.randomUUID();
    }

    private MatchingRequest request() {
        return new MatchingRequest("MATH", "LYCEE", "ONLINE",
                new BigDecimal("200"), "Casablanca", null, 60, null);
    }

    private MatchingFeatures prof(double rating, String city, long avgPriceMinor) {
        String json = String.format(
                "{\"subjects\":[\"MATH\"],\"levels\":[\"LYCEE\"],\"modalities\":[\"ONLINE\"]," +
                        "\"rating\":%s,\"city\":\"%s\",\"avg_price_minor\":%d}",
                rating, city, avgPriceMinor);
        return MatchingFeatures.builder()
                .userId(UUID.randomUUID())
                .role("PROF")
                .features(json)
                .computedAt(Instant.now())
                .version(0)
                .build();
    }

    @Test
    void match_happyPath_loggingDriver_returnsSortedSuggestions() {
        MatchingFeatures p1 = prof(4.8, "Casablanca", 15_000L); // strong
        MatchingFeatures p2 = prof(3.0, "Rabat", 30_000L);      // baseline only
        when(cacheStore.key(eq(studentId), anyString())).thenReturn("ai:match:k");
        when(cacheStore.get(anyString())).thenReturn(Optional.empty());
        when(matchingFeaturesRepository.findCandidateProfs("MATH", "LYCEE", "ONLINE"))
                .thenReturn(List.of(p1, p2));
        when(envelopeBuilder.toOutbox(any(UUID.class), anyString(), anyString(), any()))
                .thenReturn(OutboxEvent.builder().id(UUID.randomUUID()).build());

        MatchingResponse out = service.match(studentId, request());

        assertThat(out).isNotNull();
        assertThat(out.cacheHit()).isFalse();
        assertThat(out.results()).hasSize(2);
        // p1 must rank above p2 — strictly greater score.
        assertThat(out.results().get(0).profId()).isEqualTo(p1.getUserId());
        assertThat(out.results().get(0).score()).isGreaterThan(out.results().get(1).score());
        // Pricing is derived from avg_price_minor and durationMinutes.
        MatchSuggestion top = out.results().get(0);
        assertThat(top.basePriceMad()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(top.totalPriceMad()).isEqualByComparingTo(new BigDecimal("150.00"));
        // No Anthropic call in logging mode.
        verify(claudeClient, never()).messages(any(), any(UUID.class));
        // ai_calls_log row is persisted.
        verify(aiCallLogger).record(eq(studentId), eq(AiAgent.MATCHING),
                eq(MatchingService.LOCAL_MODEL_ID), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), eq(true), eq(null), any(UUID.class));
        // Outbox row is inserted.
        verify(outboxRepository).save(any(OutboxEvent.class));
        verify(envelopeBuilder).toOutbox(any(UUID.class),
                eq(MatchingService.EVENT_AI_CALL_COMPLETED),
                eq(AiAgent.MATCHING.partitionKey()), any());
        // And the response is cached.
        verify(cacheStore).put(eq("ai:match:k"), any(MatchingResponse.class));
    }

    @Test
    void match_cacheHit_shortCircuits_andDoesNotCallScoringNorOutbox() {
        MatchingResponse cached = new MatchingResponse(
                List.of(new MatchSuggestion(UUID.randomUUID(), 0.9, "cached", null, null)),
                Instant.now(), MatchingService.LOCAL_MODEL_ID, false, "deadbeef");
        when(cacheStore.key(eq(studentId), anyString())).thenReturn("ai:match:k");
        when(cacheStore.get(anyString())).thenReturn(Optional.of(cached));

        MatchingResponse out = service.match(studentId, request());

        assertThat(out.cacheHit()).isTrue();
        assertThat(out.results()).hasSize(1);
        verify(matchingFeaturesRepository, never()).findCandidateProfs(anyString(), anyString(), anyString());
        verify(aiCallLogger, never()).record(any(), any(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), anyBoolean(), any(), any(UUID.class));
        verify(outboxRepository, never()).save(any(OutboxEvent.class));
        verify(cacheStore, never()).put(anyString(), any(MatchingResponse.class));
    }

    @Test
    void match_anthropicDriver_callsClaude_andStampsRealModel() {
        properties.getMatching().setDriver("anthropic");
        when(cacheStore.key(eq(studentId), anyString())).thenReturn("ai:match:k");
        when(cacheStore.get(anyString())).thenReturn(Optional.empty());
        when(matchingFeaturesRepository.findCandidateProfs("MATH", "LYCEE", "ONLINE"))
                .thenReturn(List.of(prof(4.6, "Casablanca", 12_000L)));
        when(envelopeBuilder.toOutbox(any(UUID.class), anyString(), anyString(), any()))
                .thenReturn(OutboxEvent.builder().id(UUID.randomUUID()).build());
        when(claudeClient.messages(anyString(), any(UUID.class)))
                .thenReturn(new ClaudeClient.Result("claude-opus-4-7",
                        "Refined rationale", 100, 50, 42));

        MatchingResponse out = service.match(studentId, request());

        assertThat(out.agent()).isEqualTo("claude-opus-4-7");
        assertThat(out.results().get(0).rationale()).isEqualTo("Refined rationale");
        verify(aiCallLogger).record(eq(studentId), eq(AiAgent.MATCHING),
                eq("claude-opus-4-7"), anyString(), anyString(),
                eq(100), eq(50), anyInt(), eq(true), eq(null), any(UUID.class));
    }

    @Test
    void match_emptyCandidates_stillEmitsLogAndOutbox() {
        when(cacheStore.key(eq(studentId), anyString())).thenReturn("ai:match:k");
        when(cacheStore.get(anyString())).thenReturn(Optional.empty());
        when(matchingFeaturesRepository.findCandidateProfs("MATH", "LYCEE", "ONLINE"))
                .thenReturn(List.of());
        when(envelopeBuilder.toOutbox(any(UUID.class), anyString(), anyString(), any()))
                .thenReturn(OutboxEvent.builder().id(UUID.randomUUID()).build());

        MatchingResponse out = service.match(studentId, request());

        assertThat(out.results()).isEmpty();
        verify(aiCallLogger).record(eq(studentId), eq(AiAgent.MATCHING),
                eq(MatchingService.LOCAL_MODEL_ID), anyString(), anyString(),
                anyInt(), anyInt(), anyInt(), eq(true), eq(null), any(UUID.class));
        verify(outboxRepository).save(any(OutboxEvent.class));
    }
}

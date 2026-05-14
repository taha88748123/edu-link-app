package ma.naja7i.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ma.naja7i.ai.config.AiProperties;
import ma.naja7i.ai.domain.AiAgent;
import ma.naja7i.ai.domain.MatchingFeatures;
import ma.naja7i.ai.infrastructure.messaging.payload.AiCallCompletedPayload;
import ma.naja7i.ai.infrastructure.persistence.MatchingFeaturesRepository;
import ma.naja7i.ai.infrastructure.persistence.OutboxRepository;
import ma.naja7i.ai.infrastructure.redis.MatchingCacheStore;
import ma.naja7i.ai.infrastructure.web.dto.MatchSuggestion;
import ma.naja7i.ai.infrastructure.web.dto.MatchingRequest;
import ma.naja7i.ai.infrastructure.web.dto.MatchingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Core MATCHING pipeline:
 * <ol>
 *   <li>Hash the normalised request to derive a stable cache key.</li>
 *   <li>Return the cached response on hit.</li>
 *   <li>Otherwise query candidate profs and score each via {@link MatchScoreCalculator}.</li>
 *   <li>Optionally enrich the rationale via {@link ClaudeClient} when the
 *       {@code anthropic} driver is active; fall back to a templated
 *       rationale otherwise.</li>
 *   <li>Persist an {@code ai_calls_log} row and emit
 *       {@code naja7i.ai.call.completed.v1} via the outbox.</li>
 *   <li>Cache the response in Redis.</li>
 * </ol>
 *
 * <p>The pipeline never makes a real LLM call in the {@code logging}
 * driver — that path keeps the service operational without Anthropic
 * credentials.</p>
 */
@Service
@RequiredArgsConstructor
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    /** Model identifier stamped on local-only baseline scoring rows. */
    static final String LOCAL_MODEL_ID = "local-baseline-v1";

    static final String EVENT_AI_CALL_COMPLETED = "naja7i.ai.call.completed.v1";

    private final MatchingFeaturesRepository matchingFeaturesRepository;
    private final MatchScoreCalculator scoreCalculator;
    private final ClaudeClient claudeClient;
    private final AiCallLogger aiCallLogger;
    private final MatchingCacheStore cacheStore;
    private final OutboxRepository outboxRepository;
    private final KafkaEventEnvelopeBuilder envelopeBuilder;
    private final AiProperties properties;
    private final ObjectMapper kafkaObjectMapper;

    public MatchingResponse match(UUID studentId, MatchingRequest request) {
        long t0 = System.currentTimeMillis();
        UUID traceId = UUID.randomUUID();

        String filterHash = filterHash(studentId, request);
        String cacheKey = cacheStore.key(studentId, filterHash);

        Optional<MatchingResponse> hit = cacheStore.get(cacheKey);
        if (hit.isPresent()) {
            MatchingResponse cached = hit.get();
            log.debug("Matching cache HIT for student={} filterHash={}", studentId, filterHash);
            return new MatchingResponse(cached.results(), cached.computedAt(),
                    cached.agent(), true, filterHash);
        }

        List<MatchingFeatures> candidates = fetchCandidates(request);
        int limit = effectiveLimit(request);
        String driver = properties.getMatching().getDriver();

        List<ScoredProf> scored = new ArrayList<>(candidates.size());
        for (MatchingFeatures prof : candidates) {
            double s = scoreCalculator.score(prof, request);
            scored.add(new ScoredProf(prof, s));
        }
        scored.sort(Comparator.comparingDouble(ScoredProf::score).reversed());
        if (scored.size() > limit) {
            scored = new ArrayList<>(scored.subList(0, limit));
        }

        ClaudeClient.Result llm = null;
        if ("anthropic".equalsIgnoreCase(driver) && !scored.isEmpty()) {
            try {
                llm = claudeClient.messages(buildPrompt(request, scored), traceId);
            } catch (Exception ex) {
                log.warn("Claude refinement failed (traceId={}): {}", traceId, ex.getMessage());
            }
        }

        List<MatchSuggestion> suggestions = new ArrayList<>(scored.size());
        for (ScoredProf sp : scored) {
            suggestions.add(buildSuggestion(sp, request, llm));
        }

        String model = (llm != null) ? llm.model() : LOCAL_MODEL_ID;
        int tokensIn = llm == null ? 0 : llm.tokensIn();
        int tokensOut = llm == null ? 0 : llm.tokensOut();
        int latencyMs = (int) (System.currentTimeMillis() - t0);

        String rawInput = filterHash;
        String rawOutput = suggestions.isEmpty() ? "" : Integer.toString(suggestions.size());

        aiCallLogger.record(studentId, AiAgent.MATCHING, model,
                rawInput, rawOutput, tokensIn, tokensOut, latencyMs,
                true, null, traceId);

        AiCallCompletedPayload payload = new AiCallCompletedPayload(
                AiAgent.MATCHING.name(), model, tokensIn, tokensOut, latencyMs,
                BigDecimal.ZERO, true, null);
        outboxRepository.save(envelopeBuilder.toOutbox(
                studentId == null ? UUID.randomUUID() : studentId,
                EVENT_AI_CALL_COMPLETED,
                AiAgent.MATCHING.partitionKey(),
                payload));

        MatchingResponse response = new MatchingResponse(
                suggestions, Instant.now(), model, false, filterHash);
        cacheStore.put(cacheKey, response);
        return response;
    }

    // ---------------------------------------------------------------- helpers

    protected List<MatchingFeatures> fetchCandidates(MatchingRequest request) {
        try {
            return matchingFeaturesRepository.findCandidateProfs(
                    request.subjectCode(), request.level(), request.modality());
        } catch (Exception ex) {
            // Fallback for v1: if the native JSONB query is unsupported in the
            // current dialect we degrade gracefully to an in-memory filter.
            log.warn("findCandidateProfs failed ({}). Falling back to in-memory filter.",
                    ex.getMessage());
            List<MatchingFeatures> all = matchingFeaturesRepository.findAll();
            List<MatchingFeatures> filtered = new ArrayList<>();
            for (MatchingFeatures f : all) {
                if (!"PROF".equalsIgnoreCase(f.getRole())) continue;
                if (matchesArrays(f.getFeatures(), request)) {
                    filtered.add(f);
                }
            }
            return filtered;
        }
    }

    private boolean matchesArrays(String featuresJson, MatchingRequest req) {
        try {
            JsonNode node = kafkaObjectMapper.readTree(featuresJson);
            return contains(node.path("subjects"), req.subjectCode())
                    && contains(node.path("levels"), req.level())
                    && contains(node.path("modalities"), req.modality());
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean contains(JsonNode arr, String value) {
        if (arr == null || !arr.isArray() || value == null) return false;
        for (JsonNode el : arr) {
            if (el.isTextual() && value.equalsIgnoreCase(el.asText())) return true;
        }
        return false;
    }

    private int effectiveLimit(MatchingRequest request) {
        int max = Math.max(1, properties.getMatching().getMaxResults());
        if (request == null || request.limit() == null || request.limit() <= 0) return max;
        return Math.min(max, request.limit());
    }

    private MatchSuggestion buildSuggestion(ScoredProf sp, MatchingRequest req, ClaudeClient.Result llm) {
        BigDecimal basePrice = null;
        BigDecimal totalPrice = null;
        try {
            JsonNode node = kafkaObjectMapper.readTree(sp.prof().getFeatures());
            double avgPriceMinor = node.path("avg_price_minor").asDouble(0.0);
            if (avgPriceMinor > 0.0) {
                basePrice = BigDecimal.valueOf(avgPriceMinor)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                if (req != null && req.durationMinutes() != null && req.durationMinutes() > 0) {
                    BigDecimal hours = new BigDecimal(req.durationMinutes())
                            .divide(new BigDecimal("60"), 4, RoundingMode.HALF_UP);
                    totalPrice = basePrice.multiply(hours).setScale(2, RoundingMode.HALF_UP);
                } else {
                    totalPrice = basePrice;
                }
            }
        } catch (Exception ignored) {
            // missing / malformed features blob -> no pricing
        }
        String rationale = (llm != null && llm.content() != null && !llm.content().isBlank())
                ? llm.content()
                : templatedRationale(sp.score());
        return new MatchSuggestion(sp.prof().getUserId(), round3(sp.score()),
                rationale, basePrice, totalPrice);
    }

    private static String templatedRationale(double score) {
        if (score >= 0.9) return "Excellent match based on rating, city and budget alignment.";
        if (score >= 0.7) return "Strong match: rating and at least one of city/budget align.";
        if (score >= 0.5) return "Baseline match: filters satisfied; no bonus signals.";
        return "Marginal match: only the hard filters are satisfied.";
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private String buildPrompt(MatchingRequest request, List<ScoredProf> shortlist) {
        // The prompt purposely avoids any PII (no names, no email) — only the
        // shortlisted prof UUIDs and their numeric scores travel to Anthropic.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subject", request.subjectCode());
        body.put("level", request.level());
        body.put("modality", request.modality());
        body.put("city", request.city());
        body.put("budgetMaxMad", request.budgetMaxMad());
        List<Map<String, Object>> profs = new ArrayList<>();
        for (ScoredProf sp : shortlist) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("profId", sp.prof().getUserId().toString());
            m.put("baselineScore", sp.score());
            profs.add(m);
        }
        body.put("candidates", profs);
        try {
            return "Refine the rationale for the following shortlist (JSON): "
                    + kafkaObjectMapper.writeValueAsString(body);
        } catch (Exception ex) {
            return "shortlist size=" + shortlist.size();
        }
    }

    String filterHash(UUID studentId, MatchingRequest request) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("studentId", studentId == null ? "" : studentId.toString());
        normalized.put("subjectCode", lower(request.subjectCode()));
        normalized.put("level", lower(request.level()));
        normalized.put("modality", lower(request.modality()));
        normalized.put("budgetMaxMad",
                request.budgetMaxMad() == null ? null : request.budgetMaxMad().stripTrailingZeros().toPlainString());
        normalized.put("city", lower(request.city()));
        normalized.put("dayOfWeek", request.dayOfWeek());
        normalized.put("durationMinutes", request.durationMinutes());
        normalized.put("limit", request.limit());
        try {
            String json = kafkaObjectMapper.writeValueAsString(normalized);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute filterHash", ex);
        }
    }

    private static String lower(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }

    private record ScoredProf(MatchingFeatures prof, double score) {}
}

package ma.naja7i.ai.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import ma.naja7i.ai.config.AiProperties;
import ma.naja7i.ai.infrastructure.web.dto.MatchingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Thin JSON-over-{@link StringRedisTemplate} wrapper for the matching
 * suggestion cache. Keys follow the pattern {@code ai:match:{studentId}:{filterHash}}
 * and entries expire after {@code naja7i.ai.matching.cache-ttl-minutes}.
 *
 * <p>Every method is fail-soft: a Redis outage produces a cache miss /
 * silent write failure so the matching pipeline can keep serving from
 * Postgres alone.</p>
 */
@Component
@RequiredArgsConstructor
public class MatchingCacheStore {

    private static final Logger log = LoggerFactory.getLogger(MatchingCacheStore.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper kafkaObjectMapper;
    private final AiProperties properties;

    /** Builds the canonical cache key for a (studentId, filterHash) pair. */
    public String key(UUID studentId, String filterHash) {
        return "ai:match:" + (studentId == null ? "anon" : studentId) + ":" + filterHash;
    }

    /** Returns the cached {@link MatchingResponse} if present, empty otherwise. */
    public Optional<MatchingResponse> get(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) return Optional.empty();
            return Optional.of(kafkaObjectMapper.readValue(json, MatchingResponse.class));
        } catch (Exception ex) {
            log.warn("Redis cache GET failed for {}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    /** Persists a response with the configured TTL. Silently no-ops on failure. */
    public void put(String key, MatchingResponse value) {
        try {
            String json = kafkaObjectMapper.writeValueAsString(value);
            int ttl = properties.getMatching().getCacheTtlMinutes();
            redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(Math.max(1, ttl)));
        } catch (Exception ex) {
            log.warn("Redis cache PUT failed for {}: {}", key, ex.getMessage());
        }
    }
}

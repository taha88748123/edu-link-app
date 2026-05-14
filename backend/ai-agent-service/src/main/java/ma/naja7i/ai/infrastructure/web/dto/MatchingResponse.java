package ma.naja7i.ai.infrastructure.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * Response wrapper for the matching pipeline. Embeds the driver name and
 * a {@code cacheHit} flag so dashboards can correlate latency with the
 * cache layer. {@code filterHash} is included so clients can detect
 * cache-key collisions when debugging.
 */
public record MatchingResponse(
        List<MatchSuggestion> results,
        Instant computedAt,
        String agent,
        boolean cacheHit,
        String filterHash
) {
}

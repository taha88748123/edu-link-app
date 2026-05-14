package ma.naja7i.ai.application;

import lombok.RequiredArgsConstructor;
import ma.naja7i.ai.domain.AiAgent;
import ma.naja7i.ai.domain.AiCallLog;
import ma.naja7i.ai.infrastructure.persistence.AiCallLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Central, fail-safe writer of {@code ai_calls_log}. Runs in a separate
 * transaction ({@link Propagation#REQUIRES_NEW}) so a logging failure
 * never breaks the user-facing flow.
 *
 * <p><b>Privacy</b>: stores only SHA-256 hashes of the raw prompt /
 * response — never the plaintext bodies themselves.</p>
 */
@Component
@RequiredArgsConstructor
public class AiCallLogger {

    private static final Logger log = LoggerFactory.getLogger(AiCallLogger.class);

    private final AiCallLogRepository repository;
    private final AnthropicCostCalculator costCalculator;

    /**
     * Persists a log row. Never throws — logs a warning on failure so the
     * caller can keep serving the request.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID userId,
                       AiAgent agent,
                       String model,
                       String rawInput,
                       String rawOutput,
                       int tokensIn,
                       int tokensOut,
                       int latencyMs,
                       boolean success,
                       String errorCode,
                       UUID traceId) {
        try {
            BigDecimal cost = (success && tokensIn + tokensOut > 0)
                    ? costCalculator.compute(tokensIn, tokensOut)
                    : BigDecimal.ZERO;
            AiCallLog row = AiCallLog.builder()
                    .userId(userId)
                    .agent(agent)
                    .model(model)
                    .inputHash(sha256(rawInput))
                    .outputHash(rawOutput == null ? null : sha256(rawOutput))
                    .tokensIn(tokensIn)
                    .tokensOut(tokensOut)
                    .latencyMs(latencyMs)
                    .costUsd(cost)
                    .success(success)
                    .errorCode(errorCode)
                    .traceId(traceId)
                    .build();
            repository.save(row);
        } catch (Exception ex) {
            log.warn("Failed to persist ai_calls_log row (agent={}, model={}): {}",
                    agent, model, ex.getMessage());
        }
    }

    /** SHA-256 helper — returns the lower-case hex digest. */
    static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(
                    (input == null ? "" : input).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}

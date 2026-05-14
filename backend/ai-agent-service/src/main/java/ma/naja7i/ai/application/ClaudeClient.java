package ma.naja7i.ai.application;

import java.util.UUID;

/**
 * Strategy abstraction over Anthropic. The {@code LoggingClaudeClient}
 * returns {@code null} (the matching pipeline then falls back to its
 * deterministic baseline), while {@code AnthropicClaudeClient} performs a
 * real HTTP call.
 */
public interface ClaudeClient {

    /**
     * @return the call result, or {@code null} when the call could not be
     * executed (missing credentials, timeout, upstream failure, or
     * {@code logging} driver active).
     */
    Result messages(String prompt, UUID traceId);

    /**
     * Result of a Claude call. The {@code content} field is sensitive — never log it directly.
     */
    record Result(
            String model,
            String content,
            int tokensIn,
            int tokensOut,
            int latencyMs
    ) {
        @Override
        public String toString() {
            return "Result[model=" + model + ", tokensIn=" + tokensIn
                    + ", tokensOut=" + tokensOut + ", latencyMs=" + latencyMs + "]";
        }
    }
}

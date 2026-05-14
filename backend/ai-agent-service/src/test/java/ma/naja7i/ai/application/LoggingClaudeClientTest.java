package ma.naja7i.ai.application;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The default driver must always return null so the matching pipeline
 * can fall back to its deterministic baseline.
 */
class LoggingClaudeClientTest {

    private final LoggingClaudeClient client = new LoggingClaudeClient();

    @Test
    void messages_alwaysReturnsNull() {
        assertThat(client.messages("hello", UUID.randomUUID())).isNull();
    }

    @Test
    void messages_nullPromptHandledGracefully() {
        assertThat(client.messages(null, UUID.randomUUID())).isNull();
    }
}

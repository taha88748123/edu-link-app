package ma.naja7i.ai.domain;

/**
 * Catalogue of AI agents wired in the service. Only {@link #MATCHING} is
 * operational in v1; the others are scoped for v2 — their REST endpoints
 * return {@code 501 NOT_IMPLEMENTED}.
 */
public enum AiAgent {

    MATCHING,
    QCM,
    MODERATION,
    SMART_REPLY,
    REVISION,
    TUTOR;

    /**
     * Stable label used as the Kafka partition key for
     * {@code naja7i.ai.call.completed.v1} events.
     */
    public String partitionKey() {
        return name();
    }
}

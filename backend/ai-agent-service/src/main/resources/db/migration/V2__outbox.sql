-- =====================================================================
-- ai-agent-service / ai_db -- outbox + processed_events
--
-- processed_events: dedup table for incoming Kafka events. The handler
-- writes (event_id) inside its transaction; a duplicate triggers a PK
-- violation that the consumer interprets as "already processed, skip".
--
-- outbox: outbox pattern. Each domain decision writes a row in the same
-- transaction; the OutboxPoller publishes rows where published_at IS NULL
-- and stamps the column on success.
-- =====================================================================

CREATE TABLE IF NOT EXISTS processed_events (
    event_id      UUID         PRIMARY KEY,
    event_type    VARCHAR(100) NOT NULL,
    processed_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_processed_events_type ON processed_events(event_type);

CREATE TABLE IF NOT EXISTS outbox (
    id             UUID         PRIMARY KEY,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    partition_key  VARCHAR(100) NOT NULL,
    payload        JSONB        NOT NULL,
    published_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending
    ON outbox(created_at)
    WHERE published_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_outbox_aggregate
    ON outbox(aggregate_id);

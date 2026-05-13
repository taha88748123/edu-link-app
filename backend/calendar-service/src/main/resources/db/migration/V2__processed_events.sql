-- Dedup table for Kafka consumers (at-least-once delivery → idempotent handling).
-- The handler writes (event_id) inside its transaction; a duplicate triggers a PK
-- violation that the consumer interprets as "already processed, skip".

CREATE TABLE IF NOT EXISTS processed_events (
    event_id      UUID         PRIMARY KEY,
    event_type    VARCHAR(128) NOT NULL,
    processed_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_processed_events_type ON processed_events(event_type);

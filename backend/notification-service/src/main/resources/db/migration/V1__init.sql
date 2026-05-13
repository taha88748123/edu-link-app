-- Notification service schema
-- All enums are stored as VARCHAR (matches @Enumerated(EnumType.STRING) on the entities)

CREATE TABLE IF NOT EXISTS notification_log (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             UUID NOT NULL,
    channel             VARCHAR(16) NOT NULL CHECK (channel IN ('PUSH','SMS','WA','EMAIL')),
    template_key        VARCHAR(64) NOT NULL,
    locale              VARCHAR(8)  NOT NULL,
    subject             VARCHAR(255),
    body                TEXT,
    payload             JSONB,
    status              VARCHAR(16) NOT NULL CHECK (status IN ('QUEUED','SENT','FAILED','DELIVERED','READ')),
    provider_message_id VARCHAR(255),
    error               VARCHAR(1000),
    attempts            SMALLINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at             TIMESTAMPTZ,
    delivered_at        TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_notification_log_user_created
    ON notification_log (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_log_status_created
    ON notification_log (status, created_at);

-- Idempotency tracking for Kafka consumers
CREATE TABLE IF NOT EXISTS processed_events (
    event_id     UUID PRIMARY KEY,
    topic        VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_processed_events_topic_at
    ON processed_events (topic, processed_at);

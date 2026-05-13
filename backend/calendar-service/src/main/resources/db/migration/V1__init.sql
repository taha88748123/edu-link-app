-- =====================================================================
-- calendar-service / calendar_db — initial schema
-- Mirrors docs/architecture/database-schema.md section 3.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------------
-- availability_rules
--   Recurrence WEEKLY → uses day_of_week (0=Mon ... 6=Sun)
--   Recurrence ONE_OFF → uses date (one specific calendar day)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS availability_rules (
    id           UUID         PRIMARY KEY,
    prof_id      UUID         NOT NULL,
    recurrence   VARCHAR(16)  NOT NULL,
    day_of_week  SMALLINT,
    date         DATE,
    start_time   TIME         NOT NULL,
    end_time     TIME         NOT NULL,
    timezone     VARCHAR(50)  NOT NULL DEFAULT 'Africa/Casablanca',
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT availability_rules_recurrence_chk
        CHECK (recurrence IN ('WEEKLY','ONE_OFF')),
    CONSTRAINT availability_rules_dow_chk
        CHECK (day_of_week IS NULL OR (day_of_week BETWEEN 0 AND 6)),
    CONSTRAINT availability_rules_time_chk
        CHECK (start_time < end_time),
    CONSTRAINT availability_rules_shape_chk CHECK (
        (recurrence = 'WEEKLY'  AND day_of_week IS NOT NULL AND date IS NULL)
     OR (recurrence = 'ONE_OFF' AND date IS NOT NULL        AND day_of_week IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_availability_rules_prof  ON availability_rules(prof_id);
CREATE INDEX IF NOT EXISTS idx_availability_rules_active ON availability_rules(active);

-- ---------------------------------------------------------------------
-- slots
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS slots (
    id                       UUID         PRIMARY KEY,
    prof_id                  UUID         NOT NULL,
    start_at                 TIMESTAMPTZ  NOT NULL,
    end_at                   TIMESTAMPTZ  NOT NULL,
    modality                 VARCHAR(20)  NOT NULL,
    status                   VARCHAR(20)  NOT NULL,
    booking_id               UUID,
    generated_from_rule_id   UUID,
    reserved_until           TIMESTAMPTZ,
    version                  INTEGER      NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT slots_modality_chk
        CHECK (modality IN ('ONLINE','DOMICILE','LOCAL')),
    CONSTRAINT slots_status_chk
        CHECK (status   IN ('AVAILABLE','RESERVED_PENDING','BOOKED','BLOCKED')),
    CONSTRAINT slots_time_chk
        CHECK (start_at < end_at)
);

-- Critical: prevents two concurrent non-AVAILABLE slots for the same (prof, start).
-- AVAILABLE rows are excluded so that the materializer can leave previously-released
-- slots in place without violating the constraint.
CREATE UNIQUE INDEX IF NOT EXISTS slots_no_double_booking
    ON slots(prof_id, start_at)
    WHERE status <> 'AVAILABLE';

-- Idempotency guard for slot generation: there must be at most one row per
-- (prof_id, start_at) regardless of status. Combined with the partial index above,
-- this gives us a single source of truth per (prof_id, start_at).
CREATE UNIQUE INDEX IF NOT EXISTS slots_prof_start_unique
    ON slots(prof_id, start_at);

CREATE INDEX IF NOT EXISTS idx_slots_prof_start  ON slots(prof_id, start_at);
CREATE INDEX IF NOT EXISTS idx_slots_status      ON slots(status);
CREATE INDEX IF NOT EXISTS idx_slots_booking_id  ON slots(booking_id) WHERE booking_id IS NOT NULL;

-- ---------------------------------------------------------------------
-- calendar_sync_links (skeleton, no functional logic in v1)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS calendar_sync_links (
    user_id                  UUID         PRIMARY KEY,
    provider                 VARCHAR(20)  NOT NULL,
    refresh_token_encrypted  TEXT,
    last_sync_at             TIMESTAMPTZ,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT calendar_sync_links_provider_chk
        CHECK (provider IN ('GOOGLE','APPLE'))
);

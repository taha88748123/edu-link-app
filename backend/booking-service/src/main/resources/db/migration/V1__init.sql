-- =====================================================================
-- booking-service / booking_db — initial schema
-- Mirrors docs/architecture/database-schema.md section 5.
--
-- Money is stored in BIGINT centimes (never DECIMAL). Enums are VARCHAR
-- with CHECK constraints (status values are the ASCII-safe forms).
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------------
-- bookings
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bookings (
    id                  UUID         PRIMARY KEY,
    student_id          UUID         NOT NULL,
    prof_id             UUID         NOT NULL,
    slot_id             UUID         NOT NULL,
    subject_code        VARCHAR(20),
    level               VARCHAR(20),
    modality            VARCHAR(20),
    duration_minutes    SMALLINT,
    amount_minor        BIGINT       NOT NULL CHECK (amount_minor >= 0),
    platform_fee_minor  BIGINT       NOT NULL CHECK (platform_fee_minor >= 0),
    total_amount_minor  BIGINT       NOT NULL CHECK (total_amount_minor >= 0),
    currency            VARCHAR(3)   NOT NULL DEFAULT 'MAD',
    escrow_id           UUID,
    status              VARCHAR(30)  NOT NULL,
    scheduled_start     TIMESTAMPTZ  NOT NULL,
    scheduled_end       TIMESTAMPTZ  NOT NULL,
    actual_start        TIMESTAMPTZ,
    actual_end          TIMESTAMPTZ,
    cancel_reason       TEXT,
    cancelled_by        UUID,
    cancelled_at        TIMESTAMPTZ,
    confirmed_at        TIMESTAMPTZ,
    decline_reason      TEXT,
    declined_at         TIMESTAMPTZ,
    version             INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT bookings_status_chk
        CHECK (status IN ('DEMANDEE','ACCEPTEE_PAR_PROF','EN_COURS',
                          'TERMINEE','CONFIRMEE','ANNULEE','LITIGE')),
    CONSTRAINT bookings_modality_chk
        CHECK (modality IS NULL OR modality IN ('ONLINE','DOMICILE','LOCAL')),
    CONSTRAINT bookings_level_chk
        CHECK (level IS NULL OR level IN ('PRIMAIRE','COLLEGE','LYCEE','SUPERIEUR')),
    CONSTRAINT bookings_time_chk
        CHECK (scheduled_start < scheduled_end)
);

-- One booking row per slot (calendar-service enforces slot exclusivity; this index
-- gives us defense-in-depth at the booking level).
CREATE UNIQUE INDEX IF NOT EXISTS bookings_slot_unique     ON bookings(slot_id);
CREATE INDEX        IF NOT EXISTS idx_bookings_student     ON bookings(student_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_bookings_prof        ON bookings(prof_id, created_at DESC);
CREATE INDEX        IF NOT EXISTS idx_bookings_status_time ON bookings(status, scheduled_start);

-- ---------------------------------------------------------------------
-- booking_state_transitions (append-only audit)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS booking_state_transitions (
    id            BIGSERIAL    PRIMARY KEY,
    booking_id    UUID         NOT NULL REFERENCES bookings(id),
    from_status   VARCHAR(30),
    to_status     VARCHAR(30)  NOT NULL,
    triggered_by  UUID,
    reason        TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT booking_transitions_from_chk
        CHECK (from_status IS NULL OR from_status IN
               ('DEMANDEE','ACCEPTEE_PAR_PROF','EN_COURS','TERMINEE',
                'CONFIRMEE','ANNULEE','LITIGE')),
    CONSTRAINT booking_transitions_to_chk
        CHECK (to_status IN
               ('DEMANDEE','ACCEPTEE_PAR_PROF','EN_COURS','TERMINEE',
                'CONFIRMEE','ANNULEE','LITIGE'))
);

CREATE INDEX IF NOT EXISTS idx_booking_transitions_booking
    ON booking_state_transitions(booking_id, created_at);

-- ---------------------------------------------------------------------
-- disputes (one per booking)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS disputes (
    id                UUID         PRIMARY KEY,
    booking_id        UUID         NOT NULL UNIQUE REFERENCES bookings(id),
    opened_by         UUID         NOT NULL,
    cause             TEXT         NOT NULL,
    evidence_urls     JSONB,
    status            VARCHAR(30)  NOT NULL,
    resolution_notes  TEXT,
    resolved_by       UUID,
    resolved_at       TIMESTAMPTZ,
    version           INTEGER      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT disputes_status_chk
        CHECK (status IN ('OPEN','UNDER_REVIEW','RESOLVED_REFUND',
                          'RESOLVED_PAY_PROF','RESOLVED_SPLIT'))
);

CREATE INDEX IF NOT EXISTS idx_disputes_status ON disputes(status);

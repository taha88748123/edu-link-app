-- =====================================================================
-- payment-service / payment_db — initial schema
-- Mirrors docs/architecture/database-schema.md section 6.
--
-- Money is stored in BIGINT centimes (never DECIMAL/numeric). MAD has 2
-- decimals. Negative wallet balance is impossible at the DB level. The
-- audit_log table is append-only — modifications are rejected by trigger.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------------
-- balances (one row per user)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS balances (
    user_id        UUID         PRIMARY KEY,
    balance_minor  BIGINT       NOT NULL DEFAULT 0 CHECK (balance_minor >= 0),
    currency       VARCHAR(3)   NOT NULL DEFAULT 'MAD',
    version        INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- escrows (one row per locked booking)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS escrows (
    id                     UUID         PRIMARY KEY,
    booking_id             UUID         NOT NULL UNIQUE,
    student_id             UUID         NOT NULL,
    prof_id                UUID         NOT NULL,
    amount_minor           BIGINT       NOT NULL CHECK (amount_minor > 0),
    platform_fee_minor     BIGINT       NOT NULL CHECK (platform_fee_minor >= 0),
    prof_share_minor       BIGINT       NOT NULL CHECK (prof_share_minor >= 0),
    currency               VARCHAR(3)   NOT NULL DEFAULT 'MAD',
    status                 VARCHAR(30)  NOT NULL,
    student_refund_minor   BIGINT,
    prof_payout_minor      BIGINT,
    locked_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    released_at            TIMESTAMPTZ,
    version                INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT escrows_status_chk
        CHECK (status IN ('LOCKED','RELEASED_TO_PROF','REFUNDED_TO_STUDENT','SPLIT'))
);

CREATE INDEX IF NOT EXISTS idx_escrows_student ON escrows(student_id);
CREATE INDEX IF NOT EXISTS idx_escrows_prof    ON escrows(prof_id);
CREATE INDEX IF NOT EXISTS idx_escrows_status  ON escrows(status);

-- ---------------------------------------------------------------------
-- topup_requests (wallet top-ups awaiting admin approval)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS topup_requests (
    id                       UUID         PRIMARY KEY,
    student_id               UUID         NOT NULL,
    method                   VARCHAR(20)  NOT NULL,
    amount_minor             BIGINT       NOT NULL CHECK (amount_minor > 0),
    approved_amount_minor    BIGINT       CHECK (approved_amount_minor IS NULL OR approved_amount_minor >= 0),
    currency                 VARCHAR(3)   NOT NULL DEFAULT 'MAD',
    proof_url                VARCHAR(500),
    status                   VARCHAR(30)  NOT NULL,
    admin_notes              TEXT,
    decided_by               UUID,
    decided_at               TIMESTAMPTZ,
    version                  INTEGER      NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT topup_method_chk
        CHECK (method IN ('CASHPLUS','BANK_TRANSFER','WHATSAPP','OTHER')),
    CONSTRAINT topup_status_chk
        CHECK (status IN ('EN_ATTENTE_ADMIN','APPROVED','MODIFIED','REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_topups_student ON topup_requests(student_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_topups_status  ON topup_requests(status);

-- ---------------------------------------------------------------------
-- payouts (prof withdrawals to bank)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payouts (
    id                        UUID         PRIMARY KEY,
    prof_id                   UUID         NOT NULL,
    requested_amount_minor    BIGINT       NOT NULL CHECK (requested_amount_minor > 0),
    approved_amount_minor     BIGINT       CHECK (approved_amount_minor IS NULL OR approved_amount_minor >= 0),
    currency                  VARCHAR(3)   NOT NULL DEFAULT 'MAD',
    status                    VARCHAR(30)  NOT NULL,
    iban_ref                  VARCHAR(34),
    payment_ref               VARCHAR(100),
    admin_notes               TEXT,
    decided_by                UUID,
    requested_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    decided_at                TIMESTAMPTZ,
    paid_at                   TIMESTAMPTZ,
    version                   INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT payouts_status_chk
        CHECK (status IN ('REQUESTED','APPROVED','PAID','REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_payouts_prof   ON payouts(prof_id, requested_at DESC);
CREATE INDEX IF NOT EXISTS idx_payouts_status ON payouts(status);

-- ---------------------------------------------------------------------
-- audit_log — IMMUTABLE, append-only ledger of every balance change.
-- Triggers reject UPDATE/DELETE. This table is the audit-of-record.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         UUID         NOT NULL,
    actor_id        UUID         NOT NULL,
    operation       VARCHAR(30)  NOT NULL,
    amount_minor    BIGINT       NOT NULL,
    balance_before  BIGINT       NOT NULL,
    balance_after   BIGINT       NOT NULL,
    reference_type  VARCHAR(20)  NOT NULL,
    reference_id    UUID         NOT NULL,
    reason          TEXT,
    trace_id        UUID,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT audit_operation_chk
        CHECK (operation IN ('TOPUP','ESCROW_LOCK','ESCROW_RELEASE','REFUND','PAYOUT',
                             'ADMIN_ADJUST','ADMIN_CREDIT','ADMIN_DEBIT')),
    CONSTRAINT audit_reference_type_chk
        CHECK (reference_type IN ('BOOKING','ESCROW','TOPUP','PAYOUT','ADJUST'))
);

CREATE INDEX IF NOT EXISTS audit_log_user_idx
    ON audit_log (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS audit_log_reference_idx
    ON audit_log (reference_type, reference_id);

-- Immutability triggers — no UPDATE, no DELETE.
CREATE OR REPLACE FUNCTION reject_audit_log_modification()
RETURNS TRIGGER AS $$
BEGIN
  RAISE EXCEPTION 'audit_log is append-only — modification denied';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS audit_log_no_update ON audit_log;
CREATE TRIGGER audit_log_no_update
    BEFORE UPDATE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION reject_audit_log_modification();

DROP TRIGGER IF EXISTS audit_log_no_delete ON audit_log;
CREATE TRIGGER audit_log_no_delete
    BEFORE DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION reject_audit_log_modification();

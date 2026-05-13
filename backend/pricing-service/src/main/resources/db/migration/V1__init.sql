-- ============================================================================
-- pricing-service initial schema
-- Mirrors docs/architecture/database-schema.md section 4.
-- Amounts are stored as BIGINT centimes (minor units) to match the shared
-- ma.naja7i.shared.dto.Money semantics. Never DECIMAL for money.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- prof_pricing : single-session rate rows per (subject, level, modality, duration, day_of_week?)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS prof_pricing (
    id               UUID         PRIMARY KEY,
    prof_id          UUID         NOT NULL,
    subject_code     VARCHAR(32)  NOT NULL,
    level            VARCHAR(20)  NOT NULL,
    modality         VARCHAR(20)  NOT NULL,
    duration_minutes SMALLINT     NOT NULL,
    day_of_week      SMALLINT,
    amount_minor     BIGINT       NOT NULL,
    currency         VARCHAR(3)   NOT NULL DEFAULT 'MAD',
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    version          INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT prof_pricing_level_chk    CHECK (level    IN ('PRIMAIRE','COLLEGE','LYCEE','SUPERIEUR')),
    CONSTRAINT prof_pricing_modality_chk CHECK (modality IN ('ONLINE','DOMICILE','LOCAL')),
    CONSTRAINT prof_pricing_amount_chk   CHECK (amount_minor >= 0),
    CONSTRAINT prof_pricing_duration_chk CHECK (duration_minutes > 0),
    CONSTRAINT prof_pricing_dow_chk      CHECK (day_of_week IS NULL OR (day_of_week BETWEEN 0 AND 6))
);

-- The (prof, subject, level, modality, duration, day_of_week) tuple must be unique.
-- Postgres treats NULL as distinct in plain UNIQUE, which is exactly what we want:
-- one "fallback" row with day_of_week IS NULL plus optional per-day overrides.
CREATE UNIQUE INDEX IF NOT EXISTS uq_prof_pricing_full
    ON prof_pricing (prof_id, subject_code, level, modality, duration_minutes, day_of_week);

CREATE INDEX IF NOT EXISTS idx_prof_pricing_prof   ON prof_pricing(prof_id);
CREATE INDEX IF NOT EXISTS idx_prof_pricing_active ON prof_pricing(active);

-- ---------------------------------------------------------------------------
-- pricing_packs : multi-session bundles with a global discount.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pricing_packs (
    id                 UUID         PRIMARY KEY,
    prof_id            UUID         NOT NULL,
    subject_code       VARCHAR(32)  NOT NULL,
    level              VARCHAR(20)  NOT NULL,
    modality           VARCHAR(20)  NOT NULL,
    sessions_count     SMALLINT     NOT NULL,
    discount_pct       NUMERIC(5,2) NOT NULL DEFAULT 0,
    total_amount_minor BIGINT       NOT NULL,
    currency           VARCHAR(3)   NOT NULL DEFAULT 'MAD',
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    version            INTEGER      NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pricing_packs_level_chk     CHECK (level    IN ('PRIMAIRE','COLLEGE','LYCEE','SUPERIEUR')),
    CONSTRAINT pricing_packs_modality_chk  CHECK (modality IN ('ONLINE','DOMICILE','LOCAL')),
    CONSTRAINT pricing_packs_sessions_chk  CHECK (sessions_count > 0),
    CONSTRAINT pricing_packs_discount_chk  CHECK (discount_pct >= 0 AND discount_pct <= 100),
    CONSTRAINT pricing_packs_amount_chk    CHECK (total_amount_minor >= 0)
);

CREATE INDEX IF NOT EXISTS idx_pricing_packs_prof   ON pricing_packs(prof_id);
CREATE INDEX IF NOT EXISTS idx_pricing_packs_active ON pricing_packs(active);

-- ---------------------------------------------------------------------------
-- market_suggestions : read-only cache populated by an external batch job.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS market_suggestions (
    id                      UUID         PRIMARY KEY,
    prof_id                 UUID,
    subject_code            VARCHAR(32)  NOT NULL,
    level                   VARCHAR(20)  NOT NULL,
    modality                VARCHAR(20)  NOT NULL,
    duration_minutes        SMALLINT     NOT NULL,
    suggested_amount_minor  BIGINT       NOT NULL,
    computed_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    source                  VARCHAR(20)  NOT NULL DEFAULT 'AI',
    CONSTRAINT market_suggestions_level_chk    CHECK (level    IN ('PRIMAIRE','COLLEGE','LYCEE','SUPERIEUR')),
    CONSTRAINT market_suggestions_modality_chk CHECK (modality IN ('ONLINE','DOMICILE','LOCAL')),
    CONSTRAINT market_suggestions_amount_chk   CHECK (suggested_amount_minor >= 0)
);

CREATE INDEX IF NOT EXISTS idx_market_suggestions_prof
    ON market_suggestions(prof_id, subject_code, level, modality, duration_minutes, computed_at DESC);

-- ---------------------------------------------------------------------------
-- platform_fees : platform commission config (one active row at a time per name).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS platform_fees (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    percent     NUMERIC(5,2) NOT NULL DEFAULT 0,
    fixed_minor BIGINT       NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    valid_from  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    valid_to    TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT platform_fees_percent_chk CHECK (percent >= 0 AND percent <= 100),
    CONSTRAINT platform_fees_fixed_chk   CHECK (fixed_minor >= 0)
);

CREATE INDEX IF NOT EXISTS idx_platform_fees_active ON platform_fees(active);

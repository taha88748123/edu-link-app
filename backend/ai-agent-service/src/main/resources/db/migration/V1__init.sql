-- =====================================================================
-- ai-agent-service / ai_db -- initial schema
--
-- Tables:
--   matching_features  -- denormalised feature cache (PROF + STUDENT rows)
--   qcm_templates      -- v2 question bank (not read in v1, table reserved)
--   qcm_sessions       -- v2 prof onboarding runs (not read in v1, reserved)
--   ai_calls_log       -- append-only observability of every AI call
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------------
-- matching_features: one row per user (role = PROF or STUDENT).
-- features is a JSONB blob whose shape depends on the role (see
-- MatchingFeatures javadoc). Optimistic locking via the version column
-- protects concurrent recomputation by multiple consumer replicas.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS matching_features (
    user_id      UUID         PRIMARY KEY,
    role         VARCHAR(20)  NOT NULL,
    features     JSONB        NOT NULL,
    computed_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version      INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT matching_features_role_chk
        CHECK (role IN ('PROF','STUDENT'))
);

CREATE INDEX IF NOT EXISTS idx_matching_features_role ON matching_features(role);

-- GIN index speeds up the JSONB existence operator (features -> 'subjects' ? :code)
-- used by MatchingFeaturesRepository.findCandidateProfs.
CREATE INDEX IF NOT EXISTS idx_matching_features_features_gin
    ON matching_features USING GIN (features);

-- ---------------------------------------------------------------------
-- qcm_templates: v2 only - the table is created here so v2 code can be
-- added without a follow-up migration, but no v1 logic reads or mutates
-- these rows.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS qcm_templates (
    id           UUID         PRIMARY KEY,
    subject_code VARCHAR(20)  NOT NULL,
    level        VARCHAR(20)  NOT NULL,
    difficulty   VARCHAR(20),
    questions    JSONB        NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_qcm_templates_subject_level
    ON qcm_templates(subject_code, level);

-- ---------------------------------------------------------------------
-- qcm_sessions: v2 only - reserved table; not read in v1.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS qcm_sessions (
    id                   UUID         PRIMARY KEY,
    prof_id              UUID         NOT NULL,
    subject_code         VARCHAR(20)  NOT NULL,
    level                VARCHAR(20)  NOT NULL,
    generated_questions  JSONB        NOT NULL,
    started_at           TIMESTAMPTZ,
    finished_at          TIMESTAMPTZ,
    score                NUMERIC(5,2)
);

CREATE INDEX IF NOT EXISTS idx_qcm_sessions_prof ON qcm_sessions(prof_id);

-- ---------------------------------------------------------------------
-- ai_calls_log: append-only observability of every AI call. Stores ONLY
-- SHA-256 hashes of the prompt / response (privacy constraint). Cost is
-- stored as NUMERIC(10,6) USD with 6 decimals to avoid float drift.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_calls_log (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      UUID,
    agent        VARCHAR(20)  NOT NULL,
    model        VARCHAR(50)  NOT NULL,
    input_hash   VARCHAR(64)  NOT NULL,
    output_hash  VARCHAR(64),
    tokens_in    INTEGER      NOT NULL DEFAULT 0,
    tokens_out   INTEGER      NOT NULL DEFAULT 0,
    latency_ms   INTEGER      NOT NULL DEFAULT 0,
    cost_usd     NUMERIC(10,6) NOT NULL DEFAULT 0,
    success      BOOLEAN      NOT NULL DEFAULT TRUE,
    error_code   VARCHAR(50),
    trace_id     UUID,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ai_calls_agent_chk
        CHECK (agent IN ('MATCHING','QCM','MODERATION','SMART_REPLY','REVISION','TUTOR'))
);

CREATE INDEX IF NOT EXISTS idx_ai_calls_log_agent_created
    ON ai_calls_log(agent, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_calls_log_created
    ON ai_calls_log(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_calls_log_user
    ON ai_calls_log(user_id);

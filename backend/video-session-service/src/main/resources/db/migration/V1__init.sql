-- =====================================================================
-- video-session-service / video_db — initial schema
--
-- Control-plane storage for WebRTC sessions: one row per booking with
-- lifecycle, consent and (optional) recording metadata. Media never
-- transits this DB — it lives in LiveKit / S3.
--
-- Consent is stored as a JSONB array of {userId, at, scope} objects to
-- avoid a child table; the application appends entries and flips
-- recording_enabled when both participants have granted RECORDING.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------------
-- sessions — one row per booking (1:1)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sessions (
    id                          UUID         PRIMARY KEY,
    booking_id                  UUID         NOT NULL UNIQUE,
    prof_id                     UUID         NOT NULL,
    student_id                  UUID         NOT NULL,
    livekit_room                VARCHAR(100) NOT NULL UNIQUE,
    scheduled_start             TIMESTAMPTZ  NOT NULL,
    scheduled_end               TIMESTAMPTZ  NOT NULL,
    actual_start                TIMESTAMPTZ,
    actual_end                  TIMESTAMPTZ,
    duration_effective_seconds  INTEGER,
    recording_enabled           BOOLEAN      NOT NULL DEFAULT false,
    recording_url               VARCHAR(500),
    recording_expires_at        TIMESTAMPTZ,
    consent_given_by            JSONB,
    status                      VARCHAR(20)  NOT NULL,
    version                     INTEGER      NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT sessions_status_chk
        CHECK (status IN ('SCHEDULED','LIVE','ENDED','FAILED','EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_sessions_prof_scheduled
    ON sessions(prof_id, scheduled_start);
CREATE INDEX IF NOT EXISTS idx_sessions_student_scheduled
    ON sessions(student_id, scheduled_start);
CREATE INDEX IF NOT EXISTS idx_sessions_status_scheduled
    ON sessions(status, scheduled_start);

-- ---------------------------------------------------------------------
-- session_artifacts — whiteboard / quiz / note / code snapshots
-- Content is an opaque JSONB blob owned by the producer (frontend).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS session_artifacts (
    id          UUID         PRIMARY KEY,
    session_id  UUID         NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    type        VARCHAR(20)  NOT NULL,
    content     JSONB        NOT NULL,
    created_by  UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT session_artifact_type_chk
        CHECK (type IN ('WHITEBOARD','QUIZ','NOTE','CODE'))
);

CREATE INDEX IF NOT EXISTS idx_session_artifacts_session
    ON session_artifacts(session_id, created_at);

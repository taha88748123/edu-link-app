-- =====================================================================
-- user-profile-service / profile_db — initial schema
-- Mirrors docs/architecture/database-schema.md section 2.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------------
-- profiles
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS profiles (
    user_id            UUID PRIMARY KEY,
    role               VARCHAR(16) NOT NULL,
    display_name       VARCHAR(128),
    photo_url          VARCHAR(1024),
    bio                TEXT,
    preferred_language VARCHAR(8) NOT NULL DEFAULT 'FR',
    notification_prefs JSONB NOT NULL DEFAULT '{"push":true,"sms":true,"wa":true,"email":true}'::jsonb,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT profiles_role_chk CHECK (role IN ('ADMIN','PROF','STUDENT','PARENT'))
);

CREATE INDEX IF NOT EXISTS idx_profiles_role ON profiles(role);

-- ---------------------------------------------------------------------
-- prof_profiles
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS prof_profiles (
    user_id           UUID PRIMARY KEY REFERENCES profiles(user_id) ON DELETE CASCADE,
    status            VARCHAR(32) NOT NULL DEFAULT 'EN_ATTENTE',
    intro_video_url   VARCHAR(1024),
    years_experience  INTEGER,
    ai_qcm_score      NUMERIC(5,2),
    admin_notes       TEXT,
    validated_at      TIMESTAMPTZ,
    validated_by      UUID,
    submitted_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT prof_profiles_status_chk CHECK (status IN
        ('EN_ATTENTE','EN_TEST_IA','EN_ENTRETIEN_ADMIN','VALIDE','SUSPENDU','REJETE'))
);

CREATE INDEX IF NOT EXISTS idx_prof_profiles_status ON prof_profiles(status);

-- ---------------------------------------------------------------------
-- prof_subjects
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS prof_subjects (
    user_id      UUID NOT NULL REFERENCES prof_profiles(user_id) ON DELETE CASCADE,
    subject_code VARCHAR(32) NOT NULL,
    level        VARCHAR(16) NOT NULL,
    PRIMARY KEY (user_id, subject_code, level),
    CONSTRAINT prof_subjects_level_chk CHECK (level IN ('PRIMAIRE','COLLEGE','LYCEE','SUPERIEUR'))
);

CREATE INDEX IF NOT EXISTS idx_prof_subjects_subject ON prof_subjects(subject_code);

-- ---------------------------------------------------------------------
-- prof_modalities
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS prof_modalities (
    user_id  UUID NOT NULL REFERENCES prof_profiles(user_id) ON DELETE CASCADE,
    modality VARCHAR(16) NOT NULL,
    PRIMARY KEY (user_id, modality),
    CONSTRAINT prof_modalities_chk CHECK (modality IN ('ONLINE','DOMICILE','LOCAL'))
);

-- ---------------------------------------------------------------------
-- prof_locations
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS prof_locations (
    user_id UUID NOT NULL REFERENCES prof_profiles(user_id) ON DELETE CASCADE,
    city    VARCHAR(128) NOT NULL,
    region  VARCHAR(128),
    PRIMARY KEY (user_id, city)
);

-- ---------------------------------------------------------------------
-- prof_diplomas
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS prof_diplomas (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES prof_profiles(user_id) ON DELETE CASCADE,
    title       VARCHAR(256) NOT NULL,
    institution VARCHAR(256),
    year        INTEGER,
    file_url    VARCHAR(1024) NOT NULL,
    object_key  VARCHAR(1024) NOT NULL,
    ocr_text    TEXT,
    verified    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_prof_diplomas_user ON prof_diplomas(user_id);

-- ---------------------------------------------------------------------
-- student_profiles
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS student_profiles (
    user_id      UUID PRIMARY KEY REFERENCES profiles(user_id) ON DELETE CASCADE,
    school_level VARCHAR(16),
    school_name  VARCHAR(256),
    target_exam  VARCHAR(16),
    goals        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT student_school_level_chk CHECK (school_level IS NULL OR school_level IN
        ('PRIMAIRE','COLLEGE','LYCEE','SUPERIEUR')),
    CONSTRAINT student_target_exam_chk CHECK (target_exam IS NULL OR target_exam IN
        ('BAC','BREVET','REGIONAL','AUCUN'))
);

-- ---------------------------------------------------------------------
-- student_interests
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS student_interests (
    user_id      UUID NOT NULL REFERENCES student_profiles(user_id) ON DELETE CASCADE,
    subject_code VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, subject_code)
);

-- ---------------------------------------------------------------------
-- reviews
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reviews (
    id           UUID PRIMARY KEY,
    booking_id   UUID,
    prof_id      UUID NOT NULL,
    student_id   UUID NOT NULL,
    stars        SMALLINT NOT NULL CHECK (stars BETWEEN 1 AND 5),
    comment      TEXT,
    moderated    BOOLEAN NOT NULL DEFAULT FALSE,
    moderated_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reviews_prof_id ON reviews(prof_id);
CREATE INDEX IF NOT EXISTS idx_reviews_booking_student ON reviews(booking_id, student_id);

-- ---------------------------------------------------------------------
-- review_stats (materialized — maintained by service layer)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS review_stats (
    prof_id   UUID PRIMARY KEY,
    avg_stars NUMERIC(3,2) NOT NULL DEFAULT 0,
    count     BIGINT NOT NULL DEFAULT 0
);

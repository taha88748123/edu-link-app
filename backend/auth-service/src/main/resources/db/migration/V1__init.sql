-- ============================================================================
-- auth-service initial schema
-- See docs/architecture/database-schema.md section "1. auth-service"
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    id              UUID            PRIMARY KEY,
    email           VARCHAR(255)    UNIQUE,
    phone           VARCHAR(20)     UNIQUE NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    locale          VARCHAR(5)      NOT NULL DEFAULT 'fr',
    email_verified  BOOLEAN         NOT NULL DEFAULT FALSE,
    phone_verified  BOOLEAN         NOT NULL DEFAULT FALSE,
    two_fa_enabled  BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ,
    CONSTRAINT users_role_chk   CHECK (role   IN ('ADMIN','PROF','STUDENT','PARENT')),
    CONSTRAINT users_status_chk CHECK (status IN ('PENDING_OTP','ACTIVE','SUSPENDED','DELETED'))
);

CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_role   ON users(role);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id            UUID         PRIMARY KEY,
    user_id       UUID         NOT NULL,
    token_hash    VARCHAR(64)  NOT NULL UNIQUE,
    expires_at    TIMESTAMPTZ  NOT NULL,
    revoked_at    TIMESTAMPTZ,
    device_label  VARCHAR(100),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_user_id    ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_expires_at ON refresh_tokens(expires_at);

CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGSERIAL     PRIMARY KEY,
    user_id     UUID,
    action      VARCHAR(50)   NOT NULL,
    ip          VARCHAR(64),
    user_agent  VARCHAR(255),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_user_id   ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action    ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at);

-- Placeholder for v2 social_logins (Google/Apple)
-- CREATE TABLE social_logins (...) -- intentionally deferred to v2 per scope.

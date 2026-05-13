-- Extra indexes / housekeeping for auth_db.

CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_users_phone_unique ON users (phone);

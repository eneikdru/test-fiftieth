-- Flyway Migration V20260823023034126: Patch IAM Implementation and Tests
-- Mandatory Flyway version: V20260823023034126

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

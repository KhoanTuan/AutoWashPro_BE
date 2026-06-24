-- Patch existing DB for email-auth columns (idempotent, safe to re-run).

ALTER TABLE IF EXISTS customer ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20);
UPDATE customer SET auth_provider = 'PHONE' WHERE auth_provider IS NULL;
ALTER TABLE customer ALTER COLUMN auth_provider SET DEFAULT 'PHONE';
ALTER TABLE customer ALTER COLUMN auth_provider SET NOT NULL;
ALTER TABLE customer ALTER COLUMN phone_number DROP NOT NULL;

ALTER TABLE IF EXISTS customer ADD COLUMN IF NOT EXISTS username VARCHAR(50);
CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_username ON customer (username) WHERE username IS NOT NULL;

ALTER TABLE IF EXISTS security_token ADD COLUMN IF NOT EXISTS is_used BOOLEAN DEFAULT false;
UPDATE security_token SET is_used = COALESCE(is_used, used, false) WHERE is_used IS NULL;
ALTER TABLE security_token ALTER COLUMN is_used SET DEFAULT false;
ALTER TABLE security_token ALTER COLUMN is_used SET NOT NULL;
ALTER TABLE security_token DROP COLUMN IF EXISTS used;
ALTER TABLE security_token DROP COLUMN IF EXISTS email;
ALTER TABLE security_token DROP COLUMN IF EXISTS user_id;

ALTER TABLE security_token ALTER COLUMN customer_id DROP NOT NULL;
ALTER TABLE IF EXISTS security_token ADD COLUMN IF NOT EXISTS staff_id BIGINT;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_security_token_staff'
    ) THEN
        ALTER TABLE security_token
            ADD CONSTRAINT fk_security_token_staff
            FOREIGN KEY (staff_id) REFERENCES staff(staff_id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_security_token_staff ON security_token (staff_id);

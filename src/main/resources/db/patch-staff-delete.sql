-- Phase B: staff soft delete + admin audit log (idempotent).

ALTER TABLE IF EXISTS staff ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_staff_deleted_at ON staff (deleted_at);

CREATE TABLE IF NOT EXISTS admin_audit_log (
    audit_id        BIGSERIAL PRIMARY KEY,
    actor_staff_id  BIGINT,
    actor_username  VARCHAR(50),
    action          VARCHAR(50)  NOT NULL,
    target_type     VARCHAR(30)  NOT NULL,
    target_id       BIGINT,
    detail          VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_target ON admin_audit_log (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_created ON admin_audit_log (created_at);

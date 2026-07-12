ALTER TABLE users
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE users
SET status = 'ACTIVE'
WHERE status IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_users_status'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT chk_users_status
                CHECK (status IN ('ACTIVE', 'LOCK', 'DELETE'));
    END IF;
END $$;

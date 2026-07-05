ALTER TABLE users
    ADD COLUMN IF NOT EXISTS pro_starts_at TIMESTAMPTZ;

UPDATE users
SET pro_starts_at = created_at
WHERE pro_expires_at IS NOT NULL
  AND pro_starts_at IS NULL;

ALTER TABLE payment_orders
    ADD COLUMN IF NOT EXISTS pro_starts_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS pro_expires_at TIMESTAMPTZ;

UPDATE payment_orders
SET pro_starts_at = paid_at,
    pro_expires_at = users.pro_expires_at
FROM users
WHERE payment_orders.user_id = users.id
  AND payment_orders.status = 'PAID'
  AND payment_orders.pro_starts_at IS NULL
  AND payment_orders.pro_expires_at IS NULL;

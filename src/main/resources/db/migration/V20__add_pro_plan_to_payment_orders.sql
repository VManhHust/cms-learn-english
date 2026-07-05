ALTER TABLE payment_orders
    ADD COLUMN IF NOT EXISTS plan_code VARCHAR(20);

UPDATE payment_orders
SET plan_code = 'YEARLY'
WHERE plan_code IS NULL;

ALTER TABLE payment_orders
    ALTER COLUMN plan_code SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_orders_user_plan_status
    ON payment_orders(user_id, plan_code, status);

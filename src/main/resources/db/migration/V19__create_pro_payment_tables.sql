ALTER TABLE users
    ADD COLUMN IF NOT EXISTS pro_expires_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS payment_orders (
    id                    UUID PRIMARY KEY,
    user_id               BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    payment_code          VARCHAR(20)  NOT NULL UNIQUE,
    amount                BIGINT       NOT NULL CHECK (amount > 0),
    status                VARCHAR(20)  NOT NULL,
    expires_at            TIMESTAMPTZ  NOT NULL,
    paid_at               TIMESTAMPTZ,
    sepay_transaction_id  BIGINT UNIQUE,
    bank_reference_code   VARCHAR(255),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_orders_user_id
    ON payment_orders(user_id);

CREATE INDEX IF NOT EXISTS idx_payment_orders_payment_code
    ON payment_orders(payment_code);

CREATE TABLE IF NOT EXISTS payment_webhook_events (
    transaction_id BIGINT PRIMARY KEY,
    raw_payload    TEXT        NOT NULL,
    received_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE email_verifications (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255)        NOT NULL,
    code_hash   VARCHAR(64)         NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used        BOOLEAN             NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index để lookup nhanh theo email
CREATE INDEX idx_email_verifications_email ON email_verifications (email);

-- Create vocabulary_bank table for storing user's saved words
CREATE TABLE vocabulary_bank (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    word       VARCHAR(100) NOT NULL,
    added_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_vocabulary_bank_user_word UNIQUE (user_id, word)
);

CREATE INDEX idx_vocabulary_bank_user_added ON vocabulary_bank (user_id, added_at DESC);

COMMENT ON TABLE vocabulary_bank IS 'Stores words saved by users to their personal vocabulary bank';

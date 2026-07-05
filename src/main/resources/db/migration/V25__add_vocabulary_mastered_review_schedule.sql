ALTER TABLE user_vocabulary_word_progress
    ADD COLUMN mastered_review_stage INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN review_completed BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_user_vocab_word_due_review
    ON user_vocabulary_word_progress(user_id, status, review_completed, next_review_at);

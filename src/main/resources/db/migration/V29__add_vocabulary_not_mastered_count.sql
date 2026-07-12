ALTER TABLE user_vocabulary_word_progress
    ADD COLUMN not_mastered_count INTEGER NOT NULL DEFAULT 0;

UPDATE user_vocabulary_word_progress
SET not_mastered_count = GREATEST(review_count - correct_count, 0);

CREATE INDEX idx_user_vocab_word_not_mastered_count
    ON user_vocabulary_word_progress(user_id, not_mastered_count DESC, updated_at DESC)
    WHERE not_mastered_count > 0;

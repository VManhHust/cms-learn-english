UPDATE user_vocabulary_word_progress
SET status = CASE
    WHEN status IN ('GOOD', 'EASY', 'MASTERED') THEN 'MASTERED'
    ELSE 'NOT_MASTERED'
END,
last_rating = CASE
    WHEN last_rating IN ('GOOD', 'EASY', 'MASTERED') THEN 'MASTERED'
    WHEN last_rating IS NULL THEN NULL
    ELSE 'NOT_MASTERED'
END
WHERE status NOT IN ('MASTERED', 'NOT_MASTERED')
   OR (last_rating IS NOT NULL AND last_rating NOT IN ('MASTERED', 'NOT_MASTERED'));

ALTER TABLE user_vocabulary_word_progress
    ALTER COLUMN status SET DEFAULT 'NOT_MASTERED';

ALTER TABLE user_vocabulary_word_progress
    ADD CONSTRAINT chk_user_vocab_word_status
        CHECK (status IN ('MASTERED', 'NOT_MASTERED')),
    ADD CONSTRAINT chk_user_vocab_word_last_rating
        CHECK (last_rating IS NULL OR last_rating IN ('MASTERED', 'NOT_MASTERED'));

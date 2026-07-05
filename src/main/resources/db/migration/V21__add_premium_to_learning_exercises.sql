ALTER TABLE learning_exercise
    ADD COLUMN IF NOT EXISTS is_premium BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_learning_exercise_premium
    ON learning_exercise(is_premium);

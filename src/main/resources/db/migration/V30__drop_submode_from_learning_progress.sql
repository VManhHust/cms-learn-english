DELETE FROM learning_progress
WHERE id IN (
    SELECT id
    FROM (
        SELECT
            id,
            ROW_NUMBER() OVER (
                PARTITION BY user_id, lesson_id
                ORDER BY updated_at DESC, created_at DESC, id DESC
            ) AS row_number
        FROM learning_progress
    ) duplicate_progress
    WHERE row_number > 1
);

ALTER TABLE learning_progress
    DROP CONSTRAINT IF EXISTS uk_user_lesson_submode;

ALTER TABLE learning_progress
    ADD CONSTRAINT uk_user_lesson UNIQUE (user_id, lesson_id);

ALTER TABLE learning_progress
    DROP COLUMN IF EXISTS submode;

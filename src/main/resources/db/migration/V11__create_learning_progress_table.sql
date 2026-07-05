-- Create learning_progress table for storing user learning progress
CREATE TABLE learning_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id BIGINT NOT NULL,
    submode VARCHAR(20) NOT NULL CHECK (submode IN ('full', 'fill-blank')),
    segment_results JSONB NOT NULL DEFAULT '{}',
    user_inputs JSONB DEFAULT '{}',
    completion_percentage INTEGER NOT NULL DEFAULT 0 CHECK (completion_percentage >= 0 AND completion_percentage <= 100),
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_user_lesson_submode UNIQUE (user_id, lesson_id, submode)
);

-- Create indexes for performance optimization
CREATE INDEX idx_learning_progress_user_lesson ON learning_progress(user_id, lesson_id);
CREATE INDEX idx_learning_progress_updated_at ON learning_progress(updated_at);
CREATE INDEX idx_learning_progress_user_completed ON learning_progress(user_id, is_completed);

-- Add comment for documentation
COMMENT ON TABLE learning_progress IS 'Stores user learning progress for dictation exercises';
COMMENT ON COLUMN learning_progress.segment_results IS 'JSONB map of segment index to result data (checked, skipped, accuracy, isGood)';
COMMENT ON COLUMN learning_progress.user_inputs IS 'JSONB map of segment index to user input text';
COMMENT ON COLUMN learning_progress.completion_percentage IS 'Percentage of segments completed (0-100)';
COMMENT ON COLUMN learning_progress.is_completed IS 'Whether the exercise is marked as completed (100% progress)';
COMMENT ON COLUMN learning_progress.completed_at IS 'Timestamp when the exercise was completed';

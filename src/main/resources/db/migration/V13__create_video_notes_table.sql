-- Create video_notes table for storing user notes on video exercises
CREATE TABLE video_notes (
    id                   BIGSERIAL    PRIMARY KEY,
    user_id              BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    video_id             BIGINT       NOT NULL REFERENCES learning_topic(id) ON DELETE CASCADE,
    exercise_module_id   BIGINT       NOT NULL REFERENCES exercise_module_youtube_extension(id) ON DELETE CASCADE,
    note_content         TEXT         NOT NULL,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance optimization
CREATE INDEX idx_video_notes_user_created ON video_notes (user_id, created_at DESC);
CREATE INDEX idx_video_notes_exercise_module ON video_notes (exercise_module_id);

-- Add comments for documentation
COMMENT ON TABLE video_notes IS 'Stores user notes created while watching video exercises';
COMMENT ON COLUMN video_notes.user_id IS 'Reference to the user who created the note';
COMMENT ON COLUMN video_notes.video_id IS 'Reference to the video (learning_topic) associated with the note';
COMMENT ON COLUMN video_notes.exercise_module_id IS 'Reference to the specific exercise module (sentence) in the video';
COMMENT ON COLUMN video_notes.note_content IS 'The actual note content written by the user';
COMMENT ON COLUMN video_notes.created_at IS 'Timestamp when the note was created';

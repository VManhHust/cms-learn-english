-- Add unique constraint to ensure one note per user per exercise module
-- This prevents duplicate notes for the same user and exercise module combination
ALTER TABLE video_notes 
ADD CONSTRAINT uk_video_notes_user_exercise_module 
UNIQUE (user_id, exercise_module_id);

-- Add comment for documentation
COMMENT ON CONSTRAINT uk_video_notes_user_exercise_module ON video_notes 
IS 'Ensures each user can have only one note per exercise module';

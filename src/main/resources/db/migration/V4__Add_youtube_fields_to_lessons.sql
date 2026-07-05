-- Add YouTube fields to lessons table
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS youtube_url VARCHAR(255);
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS youtube_id VARCHAR(50);

-- Update existing lessons with sample YouTube data (optional)
-- You can remove this if you want to add data manually

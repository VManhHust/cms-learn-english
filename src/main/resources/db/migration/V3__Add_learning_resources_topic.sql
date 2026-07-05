-- Insert learning-resources topic if not exists
INSERT INTO topics (name, slug, description, created_at)
SELECT 'Learning resources', 'learning-resources', 'Tài nguyên học tập tiếng Anh', NOW()
WHERE NOT EXISTS (SELECT 1 FROM topics WHERE slug = 'learning-resources');

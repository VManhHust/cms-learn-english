-- Delete old seed data from V2
DELETE FROM lessons WHERE topic_id IN (1, 3);

-- Seed Daily English Conversation (topic_id = 1)
INSERT INTO lessons (topic_id, title, thumbnail, duration, level, view_count, source, youtube_url, youtube_id, has_dictation, has_shadowing) VALUES
  (1, 'Love Story - English Conversation Practice', NULL, '02:24', 'A2', 54644, 'Youtube', 'https://www.youtube.com/watch?v=5cKeDvaD5eI', '5cKeDvaD5eI', TRUE, TRUE),
  (1, 'Valentine''s Day Story | Culture and History Stories for Kids', NULL, '02:59', 'A2', 12069, 'Youtube', 'https://www.youtube.com/watch?v=vf6c6n35wr4', 'vf6c6n35wr4', TRUE, TRUE),
  (1, 'A Sweet Welcome - Easy English Dialogue', NULL, '00:34', 'A1', 52441, 'Youtube', 'https://www.youtube.com/watch?v=5cKeDvaD5eI', '5cKeDvaD5eI', TRUE, TRUE),
  (1, 'What day is it today? Easy Dialogue - English for Beginners', NULL, '01:32', 'A1', 35669, 'Youtube', 'https://www.youtube.com/watch?v=vf6c6n35wr4', 'vf6c6n35wr4', TRUE, TRUE);

-- Seed Movie short clip (topic_id = 3)
INSERT INTO lessons (topic_id, title, thumbnail, duration, level, view_count, source, youtube_url, youtube_id, has_dictation, has_shadowing) VALUES
  (3, 'PRINCESS MONONOKE | Official English Trailer', NULL, '02:10', 'B1', 980000, 'Youtube', 'https://www.youtube.com/watch?v=vf6c6n35wr4', 'vf6c6n35wr4', TRUE, TRUE),
  (3, 'Keenan Te - Rest of My Life (Official Lyric Video)', NULL, '03:45', 'A2', 2100000, 'Youtube', 'https://www.youtube.com/watch?v=5cKeDvaD5eI', '5cKeDvaD5eI', TRUE, TRUE);

-- Thêm các topic mới để test filter
INSERT INTO learning_topic (topic_name, description, type) VALUES
('BBC Learning English', 'Học tiếng Anh chuẩn British qua BBC', 'YOUTUBE'),
('TED Talks', 'Bài nói chuyện truyền cảm hứng từ TED', 'YOUTUBE'),
('Daily Conversation', 'Hội thoại tiếng Anh hàng ngày', 'YOUTUBE'),
('IELTS Listening', 'Luyện nghe IELTS', 'YOUTUBE'),
('Science & Facts', 'Khoa học và sự thật thú vị', 'YOUTUBE');

-- Thêm một số exercises vào topic BBC Learning English (id = 2)
INSERT INTO learning_exercise (uuid, type, title, module_count, vocabulary_level, topic_id) VALUES
('abc_bbc_001', 'YOUTUBE_VIDEO', 'How to use the present perfect', 30, 'A2', 2),
('abc_bbc_002', 'YOUTUBE_VIDEO', 'English at Work - The job interview', 28, 'B1', 2),
('abc_bbc_003', 'YOUTUBE_VIDEO', '6 Minute English - Artificial Intelligence', 35, 'B2', 2),
('abc_bbc_004', 'YOUTUBE_VIDEO', 'The English We Speak - Piece of cake', 20, 'A1', 2);

-- Thêm một số exercises vào topic TED Talks (id = 3)
INSERT INTO learning_exercise (uuid, type, title, module_count, vocabulary_level, topic_id) VALUES
('abc_ted_001', 'YOUTUBE_VIDEO', 'How great leaders inspire action | Simon Sinek', 90, 'B2', 3),
('abc_ted_002', 'YOUTUBE_VIDEO', 'The happy secret to better work | Shawn Achor', 85, 'B1', 3),
('abc_ted_003', 'YOUTUBE_VIDEO', 'Inside the mind of a master procrastinator | Tim Urban', 95, 'C1', 3),
('abc_ted_004', 'YOUTUBE_VIDEO', 'What makes a good life? | Robert Waldinger', 80, 'B2', 3);

-- Extensions cho BBC exercises (dùng video BBC thật)
INSERT INTO learning_exercise_youtube_extension (video_id, thumbnail_url, duration_seconds, learning_exercise_id, youtube_channel_id) VALUES
('sY9j3-3W5Bs', 'https://i.ytimg.com/vi/sY9j3-3W5Bs/mqdefault.jpg', 180, 10, 1),
('Vc9H7g6IQKU', 'https://i.ytimg.com/vi/Vc9H7g6IQKU/mqdefault.jpg', 210, 11, 1),
('Hs0557QLQR4', 'https://i.ytimg.com/vi/Hs0557QLQR4/mqdefault.jpg', 360, 12, 1),
('WLmYGUMFkxs', 'https://i.ytimg.com/vi/WLmYGUMFkxs/mqdefault.jpg', 150, 13, 1);

-- Extensions cho TED exercises
INSERT INTO learning_exercise_youtube_extension (video_id, thumbnail_url, duration_seconds, learning_exercise_id, youtube_channel_id) VALUES
('qp0HIF3SfI4', 'https://i.ytimg.com/vi/qp0HIF3SfI4/mqdefault.jpg', 1080, 14, 2),
('fLJsdqxnZb0', 'https://i.ytimg.com/vi/fLJsdqxnZb0/mqdefault.jpg', 780, 15, 2),
('arj7oStGLkU', 'https://i.ytimg.com/vi/arj7oStGLkU/mqdefault.jpg', 855, 16, 2),
('q-7zAkwAOYg', 'https://i.ytimg.com/vi/q-7zAkwAOYg/mqdefault.jpg', 750, 17, 2);

-- ── YouTube Channels ─────────────────────────────────────────────────────────
INSERT INTO youtube_channels (channel_youtube_id, channel_name, channel_img_url, channel_description, channel_subscriber_count) VALUES
('UCCj956IF62FbT7Gouszaj9w', 'BBC',
 'https://yt3.ggpht.com/2kUet1z3iIsuYaUEPuW0uMDVt9tRAzciL-N_pgy4bAvMazAfAgbJPrGrR3k8MhnGp-GGwy3O=s240-c-k-c0x00ffffff-no-rj',
 'The BBC is the world''s leading public service broadcaster.',
 14600000),
('UCsooa4yRKGN_zEE8iknghZA', 'TED',
 'https://yt3.ggpht.com/ytc/AIdro_mTZ57zBDvFRCBMbMBFMFJGMFJGMFJGMFJGMFJG=s240-c-k-c0x00ffffff-no-rj',
 'TED is a nonprofit devoted to spreading ideas, usually in the form of short, powerful talks.',
 19800000),
('UCsT0YIqwnpJCM-mx7-gSA4Q', 'TEDx Talks',
 'https://yt3.ggpht.com/ytc/AIdro_mTZ57zBDvFRCBMbMBFMFJGMFJGMFJGMFJGMFJG=s240-c-k-c0x00ffffff-no-rj',
 'TEDx is an international community that organizes TED-style events anywhere and everywhere.',
 38200000),
('UCznv7Vf9nBdJYvBagFdAHWw', 'Kurzgesagt – In a Nutshell',
 'https://yt3.ggpht.com/ytc/AIdro_mTZ57zBDvFRCBMbMBFMFJGMFJGMFJGMFJGMFJG=s240-c-k-c0x00ffffff-no-rj',
 'Videos explaining things with optimistic nihilism.',
 22100000);

-- ── Learning Exercises (YouTube Videos) ──────────────────────────────────────
-- topic_id = 1 (YOUTUBE topic seeded in V7)

INSERT INTO learning_exercise (uuid, type, title, module_count, vocabulary_level, topic_id) VALUES
-- BBC videos
('mKoLacfklLM', 'YOUTUBE_VIDEO', 'What would you do if a volcano appeared in your back garden?', 85, 'B1', 1),
('dnCJ1e-aaAk', 'YOUTUBE_VIDEO', 'Why do we dream?', 72, 'B2', 1),
('RTr2X-CMpCI', 'YOUTUBE_VIDEO', 'The science of sleep', 68, 'B1', 1),
-- TED videos
('eIho2S0ZahI', 'YOUTUBE_VIDEO', 'Do schools kill creativity? | Sir Ken Robinson', 120, 'C1', 1),
('6Af6b_wyiwI', 'YOUTUBE_VIDEO', 'The power of vulnerability | Brené Brown', 115, 'B2', 1),
('H14bBuluwB8', 'YOUTUBE_VIDEO', 'Your body language may shape who you are | Amy Cuddy', 98, 'B2', 1),
-- Kurzgesagt videos
('BtN-goy9VOY', 'YOUTUBE_VIDEO', 'Optimistic Nihilism', 65, 'B2', 1),
('zQGOcOUBi6s', 'YOUTUBE_VIDEO', 'What Is Something?', 58, 'C1', 1),
('sNhhvQGsMEc', 'YOUTUBE_VIDEO', 'The Egg – A Short Story', 42, 'B1', 1);

-- ── YouTube Exercise Extensions ───────────────────────────────────────────────
INSERT INTO learning_exercise_youtube_extension (video_id, thumbnail_url, duration_seconds, learning_exercise_id, youtube_channel_id) VALUES
-- BBC (channel id = 1)
('mKoLacfklLM', 'https://i.ytimg.com/vi/mKoLacfklLM/mqdefault.jpg', 145,  1, 1),
('dnCJ1e-aaAk', 'https://i.ytimg.com/vi/dnCJ1e-aaAk/mqdefault.jpg', 213,  2, 1),
('RTr2X-CMpCI', 'https://i.ytimg.com/vi/RTr2X-CMpCI/mqdefault.jpg', 187,  3, 1),
-- TED (channel id = 2)
('eIho2S0ZahI', 'https://i.ytimg.com/vi/eIho2S0ZahI/mqdefault.jpg', 1147, 4, 2),
('6Af6b_wyiwI', 'https://i.ytimg.com/vi/6Af6b_wyiwI/mqdefault.jpg', 1320, 5, 2),
('H14bBuluwB8', 'https://i.ytimg.com/vi/H14bBuluwB8/mqdefault.jpg', 1264, 6, 2),
-- Kurzgesagt (channel id = 4)
('BtN-goy9VOY', 'https://i.ytimg.com/vi/BtN-goy9VOY/mqdefault.jpg', 421,  7, 4),
('zQGOcOUBi6s', 'https://i.ytimg.com/vi/zQGOcOUBi6s/mqdefault.jpg', 388,  8, 4),
('sNhhvQGsMEc', 'https://i.ytimg.com/vi/sNhhvQGsMEc/mqdefault.jpg', 256,  9, 4);

-- ── Exercise Module YouTube Extensions (transcript segments) ──────────────────
-- Video: mKoLacfklLM (BBC volcano video) — 10 segments mẫu
INSERT INTO exercise_module_youtube_extension (time_start_ms, time_end_ms, correct_answer) VALUES
(0,     2560,  'What would you do if a volcano'),
(2560,  3840,  'suddenly appeared'),
(4200,  5400,  'in your garden?'),
(6560,  7480,  'Oh, God. Jesus.'),
(9760,  12120, 'Well, my fella''s Australian, so I feel like he''d like,'),
(12120, 13520, 'put a grill on it. Nice!'),
(13520, 15760, 'Crack open a few tinnies. Yeah.'),
(16040, 18640, 'A couple of chooks on the barbecue.'),
(18640, 22360, 'Haven''t got the accent. No, no, I was like finding my way.'),
(22360, 26840, 'We''re gonna put some chooks on the barbie. Crack open a few tinnies.');

-- Video: eIho2S0ZahI (TED Ken Robinson) — 10 segments mẫu
INSERT INTO exercise_module_youtube_extension (time_start_ms, time_end_ms, correct_answer) VALUES
(1000,  5000,  'Good morning. How are you?'),
(5000,  10000, 'It''s been a great conference, hasn''t it?'),
(10000, 16000, 'I have been blown away by the whole thing.'),
(16000, 22000, 'In fact, I''m leaving. There have been three themes running through the conference'),
(22000, 28000, 'which are relevant to what I want to talk about.'),
(28000, 35000, 'One is the extraordinary evidence of human creativity'),
(35000, 42000, 'in all of the presentations that we''ve had and in all of the people here.'),
(42000, 50000, 'Just the variety and the range of it has been itself a testimony'),
(50000, 57000, 'to something I''m going to talk about.'),
(57000, 65000, 'The second is that it''s put us in a place where we have no idea what''s going to happen');

-- Video: BtN-goy9VOY (Kurzgesagt Optimistic Nihilism) — 10 segments mẫu
INSERT INTO exercise_module_youtube_extension (time_start_ms, time_end_ms, correct_answer) VALUES
(0,     4000,  'We are the descendants of survivors.'),
(4000,  9000,  'For billions of years, your ancestors managed to stay alive'),
(9000,  14000, 'long enough to have offspring.'),
(14000, 20000, 'Every single one of them, without exception.'),
(20000, 26000, 'This is a remarkable achievement.'),
(26000, 33000, 'But it also means that you are the product of an unbroken chain'),
(33000, 40000, 'of living things that were good enough at surviving'),
(40000, 47000, 'to pass on their genes to the next generation.'),
(47000, 54000, 'You are here because your ancestors were survivors.'),
(54000, 61000, 'And now you get to decide what to do with that.');

-- ── Exercise Modules ──────────────────────────────────────────────────────────
-- exercise_id=1 (BBC volcano), youtube_module_extension_id = 1..10
INSERT INTO exercise_module (type, exercise_id, youtube_module_extension_id) VALUES
('YOUTUBE', 1, 1), ('YOUTUBE', 1, 2), ('YOUTUBE', 1, 3), ('YOUTUBE', 1, 4), ('YOUTUBE', 1, 5),
('YOUTUBE', 1, 6), ('YOUTUBE', 1, 7), ('YOUTUBE', 1, 8), ('YOUTUBE', 1, 9), ('YOUTUBE', 1, 10);

-- exercise_id=4 (TED Ken Robinson), youtube_module_extension_id = 11..20
INSERT INTO exercise_module (type, exercise_id, youtube_module_extension_id) VALUES
('YOUTUBE', 4, 11), ('YOUTUBE', 4, 12), ('YOUTUBE', 4, 13), ('YOUTUBE', 4, 14), ('YOUTUBE', 4, 15),
('YOUTUBE', 4, 16), ('YOUTUBE', 4, 17), ('YOUTUBE', 4, 18), ('YOUTUBE', 4, 19), ('YOUTUBE', 4, 20);

-- exercise_id=7 (Kurzgesagt), youtube_module_extension_id = 21..30
INSERT INTO exercise_module (type, exercise_id, youtube_module_extension_id) VALUES
('YOUTUBE', 7, 21), ('YOUTUBE', 7, 22), ('YOUTUBE', 7, 23), ('YOUTUBE', 7, 24), ('YOUTUBE', 7, 25),
('YOUTUBE', 7, 26), ('YOUTUBE', 7, 27), ('YOUTUBE', 7, 28), ('YOUTUBE', 7, 29), ('YOUTUBE', 7, 30);

-- Update module_count cho 3 exercise có segments
UPDATE learning_exercise SET module_count = 10 WHERE id IN (1, 4, 7);

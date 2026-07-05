CREATE TABLE IF NOT EXISTS topics (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    thumbnail   VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS lessons (
    id          BIGSERIAL PRIMARY KEY,
    topic_id    BIGINT NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    title       VARCHAR(500) NOT NULL,
    thumbnail   VARCHAR(500),
    duration    VARCHAR(20),
    level       VARCHAR(10) NOT NULL DEFAULT 'A1',
    view_count  BIGINT NOT NULL DEFAULT 0,
    source      VARCHAR(50) DEFAULT 'Youtube',
    has_dictation  BOOLEAN NOT NULL DEFAULT TRUE,
    has_shadowing  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_lessons_topic_id ON lessons(topic_id);

-- Seed topics
INSERT INTO topics (name, slug, description) VALUES
  ('Daily English Conversation', 'daily-english-conversation', 'Giao tiep tieng Anh hang ngay'),
  ('IPA', 'ipa', 'Luyen phat am quoc te'),
  ('Movie short clip', 'movie-short-clip', 'Hoc tieng Anh qua phim ngan'),
  ('BBC Learning English', 'bbc-learning-english', 'Hoc tieng Anh qua BBC'),
  ('IELTS Listening', 'ielts-listening', 'Luyen nghe IELTS'),
  ('TOEIC Listening', 'toeic-listening', 'Luyen nghe TOEIC'),
  ('Science and Facts', 'science-and-facts', 'Khoa hoc va su that'),
  ('TED', 'ted', 'TED Talks')
ON CONFLICT (slug) DO NOTHING;

-- Seed lessons for Daily English Conversation
INSERT INTO lessons (topic_id, title, thumbnail, duration, level, view_count, source) VALUES
  (1, 'Love mom', NULL, '02:24', 'A2', 54644, 'Youtube'),
  (1, 'Valentine''s Day Story | Culture and History Stories for Kids', NULL, '02:59', 'A2', 12069, 'Youtube'),
  (1, 'A Sweet Welcome', NULL, '00:34', 'A1', 52441, 'Youtube'),
  (1, '[Day] What day is it today? It''s Wednesday. Easy Dialogue - English vid...', NULL, '01:32', 'A1', 35669, 'Youtube')
ON CONFLICT DO NOTHING;

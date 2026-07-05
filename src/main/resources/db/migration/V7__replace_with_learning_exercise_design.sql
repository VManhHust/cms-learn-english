-- Drop old tables (order matters due to FK constraints)
DROP TABLE IF EXISTS transcript_segments CASCADE;
DROP TABLE IF EXISTS lessons CASCADE;
DROP TABLE IF EXISTS topics CASCADE;

-- learning_topic
CREATE TABLE learning_topic (
    id          BIGSERIAL PRIMARY KEY,
    topic_name  VARCHAR(255) NOT NULL,
    description TEXT,
    type        VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- youtube_channels
CREATE TABLE youtube_channels (
    id                       BIGSERIAL PRIMARY KEY,
    channel_youtube_id       VARCHAR(255) NOT NULL UNIQUE,
    channel_name             VARCHAR(255),
    channel_img_url          VARCHAR(500),
    channel_description      TEXT,
    channel_subscriber_count BIGINT,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- learning_exercise
CREATE TABLE learning_exercise (
    id               BIGSERIAL PRIMARY KEY,
    uuid             VARCHAR(255) NOT NULL UNIQUE,
    type             VARCHAR(50)  NOT NULL,
    title            VARCHAR(500) NOT NULL,
    module_count     INT          NOT NULL DEFAULT 0,
    vocabulary_level VARCHAR(10),
    topic_id         BIGINT       NOT NULL REFERENCES learning_topic(id) ON DELETE RESTRICT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_learning_exercise_topic_id ON learning_exercise(topic_id);

-- learning_exercise_youtube_extension
CREATE TABLE learning_exercise_youtube_extension (
    id                   BIGSERIAL PRIMARY KEY,
    video_id             VARCHAR(50)  NOT NULL,
    thumbnail_url        VARCHAR(500),
    duration_seconds     INT,
    learning_exercise_id BIGINT NOT NULL UNIQUE REFERENCES learning_exercise(id) ON DELETE CASCADE,
    youtube_channel_id   BIGINT REFERENCES youtube_channels(id) ON DELETE SET NULL
);

CREATE INDEX idx_yt_ext_channel_id ON learning_exercise_youtube_extension(youtube_channel_id);

-- exercise_module_youtube_extension
CREATE TABLE exercise_module_youtube_extension (
    id             BIGSERIAL PRIMARY KEY,
    time_start_ms  INT  NOT NULL,
    time_end_ms    INT  NOT NULL,
    correct_answer TEXT NOT NULL
);

-- exercise_module
CREATE TABLE exercise_module (
    id                          BIGSERIAL PRIMARY KEY,
    type                        VARCHAR(50) NOT NULL,
    exercise_id                 BIGINT NOT NULL REFERENCES learning_exercise(id) ON DELETE CASCADE,
    youtube_module_extension_id BIGINT UNIQUE REFERENCES exercise_module_youtube_extension(id) ON DELETE CASCADE
);

CREATE INDEX idx_exercise_module_exercise_id ON exercise_module(exercise_id);

-- Seed: default YOUTUBE topic
INSERT INTO learning_topic (topic_name, description, type)
VALUES ('Youtube', 'Học tiếng Anh qua video YouTube', 'YOUTUBE');

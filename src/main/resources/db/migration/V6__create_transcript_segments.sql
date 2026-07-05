CREATE TABLE transcript_segments (
    id            BIGSERIAL PRIMARY KEY,
    lesson_id     BIGINT       NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    segment_index INT          NOT NULL,
    start_time    DOUBLE PRECISION NOT NULL,
    end_time      DOUBLE PRECISION NOT NULL,
    text          TEXT         NOT NULL,
    translation   TEXT
);

CREATE INDEX idx_transcript_segments_lesson_id ON transcript_segments(lesson_id);

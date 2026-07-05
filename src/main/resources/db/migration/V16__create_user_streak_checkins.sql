CREATE TABLE user_streak_checkins (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    check_in_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_streak_checkin_date UNIQUE (user_id, check_in_date)
);

CREATE INDEX idx_user_streak_checkins_user_date
    ON user_streak_checkins(user_id, check_in_date DESC);

COMMENT ON TABLE user_streak_checkins IS 'Stores one daily streak check-in per user';

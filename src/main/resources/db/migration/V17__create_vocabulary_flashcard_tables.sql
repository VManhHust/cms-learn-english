CREATE TABLE vocabulary_deck (
    id            BIGSERIAL PRIMARY KEY,
    slug          VARCHAR(120) NOT NULL UNIQUE,
    title         VARCHAR(255) NOT NULL,
    category      VARCHAR(120) NOT NULL,
    description   TEXT,
    cover_color   VARCHAR(30)  NOT NULL DEFAULT '#2f356d',
    status        VARCHAR(30)  NOT NULL DEFAULT 'PUBLISHED',
    is_premium    BOOLEAN      NOT NULL DEFAULT FALSE,
    learner_count INTEGER      NOT NULL DEFAULT 0,
    sort_order    INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE vocabulary_topic (
    id            BIGSERIAL PRIMARY KEY,
    deck_id       BIGINT       NOT NULL REFERENCES vocabulary_deck(id) ON DELETE CASCADE,
    slug          VARCHAR(120) NOT NULL,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    thumbnail_url TEXT,
    sort_order    INTEGER     NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_vocabulary_topic_deck_slug UNIQUE (deck_id, slug)
);

CREATE TABLE vocabulary_word (
    id                     BIGSERIAL PRIMARY KEY,
    topic_id               BIGINT       NOT NULL REFERENCES vocabulary_topic(id) ON DELETE CASCADE,
    word                   VARCHAR(160) NOT NULL,
    part_of_speech         VARCHAR(80)  NOT NULL,
    ipa_us                 VARCHAR(120),
    ipa_uk                 VARCHAR(120),
    audio_us_url           TEXT,
    audio_uk_url           TEXT,
    english_definition     TEXT         NOT NULL,
    vietnamese_definition  TEXT         NOT NULL,
    vietnamese_translation VARCHAR(255) NOT NULL,
    example_sentence       TEXT,
    example_sentence_vi    TEXT,
    image_url              TEXT,
    sort_order             INTEGER      NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_vocabulary_word_topic_word UNIQUE (topic_id, word)
);

CREATE TABLE user_vocabulary_topic_progress (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id              BIGINT      NOT NULL REFERENCES vocabulary_topic(id) ON DELETE CASCADE,
    learned_words         INTEGER     NOT NULL DEFAULT 0,
    current_word_index    INTEGER     NOT NULL DEFAULT 0,
    completion_percentage INTEGER     NOT NULL DEFAULT 0,
    is_completed          BOOLEAN     NOT NULL DEFAULT FALSE,
    completed_at          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_vocabulary_topic_progress UNIQUE (user_id, topic_id)
);

CREATE TABLE user_vocabulary_word_progress (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    word_id        BIGINT       NOT NULL REFERENCES vocabulary_word(id) ON DELETE CASCADE,
    status         VARCHAR(30)  NOT NULL DEFAULT 'NEW',
    last_rating    VARCHAR(30),
    review_count   INTEGER      NOT NULL DEFAULT 0,
    correct_count  INTEGER      NOT NULL DEFAULT 0,
    ease_factor    NUMERIC(4,2) NOT NULL DEFAULT 2.50,
    next_review_at TIMESTAMPTZ,
    learned_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_vocabulary_word_progress UNIQUE (user_id, word_id)
);

CREATE INDEX idx_vocabulary_deck_category_sort ON vocabulary_deck(category, sort_order);
CREATE INDEX idx_vocabulary_topic_deck_sort ON vocabulary_topic(deck_id, sort_order);
CREATE INDEX idx_vocabulary_word_topic_sort ON vocabulary_word(topic_id, sort_order);
CREATE INDEX idx_user_vocab_topic_user ON user_vocabulary_topic_progress(user_id, updated_at DESC);
CREATE INDEX idx_user_vocab_word_user_status ON user_vocabulary_word_progress(user_id, status, next_review_at);

INSERT INTO vocabulary_deck (slug, title, category, description, cover_color, status, is_premium, learner_count, sort_order)
VALUES
    ('1000-tu-tieng-anh-thong-dung', '1000 từ tiếng Anh thông dụng', 'Từ Vựng Tiếng Anh Thông Dụng', 'Bộ từ vựng nền tảng theo chủ đề cho người mới học.', '#2f356d', 'PUBLISHED', FALSE, 161000, 1),
    ('tu-vung-tieng-anh-giao-tiep', 'Từ vựng tiếng Anh giao tiếp', 'Từ Vựng Tiếng Anh Thông Dụng', 'Từ vựng thường gặp trong giao tiếp hằng ngày.', '#d1952d', 'PUBLISHED', FALSE, 33000, 2),
    ('600-tu-vung-ielts-co-ban', '600 từ vựng IELTS cơ bản', 'Từ Vựng IELTS', 'Các từ vựng IELTS nền tảng theo nhóm nghĩa.', '#1f9fb5', 'PUBLISHED', FALSE, 12000, 3),
    ('thanh-ngu-ielts-thong-dung', 'Thành Ngữ IELTS Thông Dụng', 'Từ Vựng IELTS', 'Các cụm thành ngữ phổ biến trong IELTS Speaking/Writing.', '#3ca35b', 'PUBLISHED', TRUE, 8600, 4);

WITH common_deck AS (
    SELECT id FROM vocabulary_deck WHERE slug = '1000-tu-tieng-anh-thong-dung'
)
INSERT INTO vocabulary_topic (deck_id, slug, title, description, thumbnail_url, sort_order)
SELECT id, 'gia-dinh', 'Gia đình', 'Từ vựng về các thành viên và mối quan hệ trong gia đình.', 'https://placehold.co/160x160/bfefff/2f356d?text=Family', 1 FROM common_deck
UNION ALL SELECT id, 'truong-hoc', 'Trường học', 'Từ vựng thường gặp trong lớp học và môi trường học tập.', 'https://placehold.co/160x160/bfefff/2f356d?text=School', 2 FROM common_deck
UNION ALL SELECT id, 'cong-viec', 'Công việc', 'Từ vựng về công sở, nhiệm vụ và nghề nghiệp.', 'https://placehold.co/160x160/bfefff/2f356d?text=Work', 3 FROM common_deck
UNION ALL SELECT id, 'nghe-nghiep', 'Nghề nghiệp', 'Tên nghề và vai trò phổ biến.', 'https://placehold.co/160x160/bfefff/2f356d?text=Jobs', 4 FROM common_deck
UNION ALL SELECT id, 'thuc-an-do-uong', 'Thức ăn & Đồ uống', 'Từ vựng về món ăn, đồ uống và bữa ăn.', 'https://placehold.co/160x160/bfefff/2f356d?text=Food', 5 FROM common_deck
UNION ALL SELECT id, 'du-lich', 'Du lịch', 'Từ vựng cần thiết khi đi lại và du lịch.', 'https://placehold.co/160x160/bfefff/2f356d?text=Travel', 6 FROM common_deck
UNION ALL SELECT id, 'mua-sam', 'Mua sắm', 'Từ vựng khi mua hàng, thanh toán và hỏi giá.', 'https://placehold.co/160x160/bfefff/2f356d?text=Shop', 7 FROM common_deck
UNION ALL SELECT id, 'suc-khoe', 'Sức khỏe', 'Từ vựng về cơ thể, triệu chứng và chăm sóc sức khỏe.', 'https://placehold.co/160x160/bfefff/2f356d?text=Health', 8 FROM common_deck;

WITH family_topic AS (
    SELECT vt.id FROM vocabulary_topic vt
    JOIN vocabulary_deck vd ON vd.id = vt.deck_id
    WHERE vd.slug = '1000-tu-tieng-anh-thong-dung' AND vt.slug = 'gia-dinh'
)
INSERT INTO vocabulary_word (topic_id, word, part_of_speech, ipa_us, ipa_uk, english_definition, vietnamese_definition, vietnamese_translation, example_sentence, example_sentence_vi, image_url, sort_order)
SELECT id, 'siblings', 'Noun', '/ˈsɪblɪŋz/', '/ˈsɪblɪŋz/', 'A person''s brothers and sisters; children with the same parents.', 'Anh chị em ruột của một người; những đứa trẻ có cùng cha mẹ.', 'anh chị em ruột', 'I have two siblings, a brother and a sister.', 'Tôi có hai anh chị em, một anh trai và một em gái.', 'https://placehold.co/240x180/bfefff/2f356d?text=Siblings', 1 FROM family_topic
UNION ALL SELECT id, 'parents', 'Noun', '/ˈperənts/', '/ˈpeərənts/', 'A mother and father, or the people who take care of a child.', 'Cha mẹ hoặc những người chăm sóc một đứa trẻ.', 'cha mẹ', 'My parents taught me to be kind.', 'Cha mẹ tôi dạy tôi phải tử tế.', 'https://placehold.co/240x180/bfefff/2f356d?text=Parents', 2 FROM family_topic
UNION ALL SELECT id, 'relative', 'Noun', '/ˈrelətɪv/', '/ˈrelətɪv/', 'A person who belongs to the same family as someone else.', 'Một người thuộc cùng gia đình với người khác.', 'họ hàng', 'She visited a relative in another city.', 'Cô ấy đến thăm một người họ hàng ở thành phố khác.', 'https://placehold.co/240x180/bfefff/2f356d?text=Relative', 3 FROM family_topic;

WITH school_topic AS (
    SELECT vt.id FROM vocabulary_topic vt
    JOIN vocabulary_deck vd ON vd.id = vt.deck_id
    WHERE vd.slug = '1000-tu-tieng-anh-thong-dung' AND vt.slug = 'truong-hoc'
)
INSERT INTO vocabulary_word (topic_id, word, part_of_speech, ipa_us, ipa_uk, english_definition, vietnamese_definition, vietnamese_translation, example_sentence, example_sentence_vi, image_url, sort_order)
SELECT id, 'assignment', 'Noun', '/əˈsaɪnmənt/', '/əˈsaɪnmənt/', 'A task or piece of work given to someone as part of a course or job.', 'Bài tập hoặc nhiệm vụ được giao trong khóa học hoặc công việc.', 'bài tập được giao', 'The teacher gave us a writing assignment.', 'Giáo viên giao cho chúng tôi một bài tập viết.', 'https://placehold.co/240x180/bfefff/2f356d?text=Assignment', 1 FROM school_topic
UNION ALL SELECT id, 'classmate', 'Noun', '/ˈklæsmeɪt/', '/ˈklɑːsmeɪt/', 'Someone who studies in the same class as you.', 'Người học cùng lớp với bạn.', 'bạn cùng lớp', 'My classmate helped me understand the lesson.', 'Bạn cùng lớp giúp tôi hiểu bài học.', 'https://placehold.co/240x180/bfefff/2f356d?text=Classmate', 2 FROM school_topic;

WITH work_topic AS (
    SELECT vt.id FROM vocabulary_topic vt
    JOIN vocabulary_deck vd ON vd.id = vt.deck_id
    WHERE vd.slug = '1000-tu-tieng-anh-thong-dung' AND vt.slug = 'cong-viec'
)
INSERT INTO vocabulary_word (topic_id, word, part_of_speech, ipa_us, ipa_uk, english_definition, vietnamese_definition, vietnamese_translation, example_sentence, example_sentence_vi, image_url, sort_order)
SELECT id, 'deadline', 'Noun', '/ˈdedlaɪn/', '/ˈdedlaɪn/', 'The latest time or date by which something must be completed.', 'Thời hạn cuối cùng mà một việc phải được hoàn thành.', 'hạn chót', 'We must finish the report before the deadline.', 'Chúng tôi phải hoàn thành báo cáo trước hạn chót.', 'https://placehold.co/240x180/bfefff/2f356d?text=Deadline', 1 FROM work_topic;

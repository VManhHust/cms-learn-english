WITH common_deck AS (
    SELECT id
    FROM vocabulary_deck
    WHERE slug = '1000-tu-tieng-anh-thong-dung'
)
INSERT INTO vocabulary_topic (deck_id, slug, title, description, thumbnail_url, sort_order)
SELECT
    id,
    'oxford-sandbox',
    'Oxford Sandbox',
    'A-words for testing Oxford Dictionaries Sandbox pronunciation audio.',
    'https://placehold.co/160x160/bfefff/2f356d?text=Oxford',
    9
FROM common_deck
ON CONFLICT (deck_id, slug) DO NOTHING;

WITH sandbox_topic AS (
    SELECT vt.id
    FROM vocabulary_topic vt
    JOIN vocabulary_deck vd ON vd.id = vt.deck_id
    WHERE vd.slug = '1000-tu-tieng-anh-thong-dung'
      AND vt.slug = 'oxford-sandbox'
)
INSERT INTO vocabulary_word (
    topic_id,
    word,
    part_of_speech,
    ipa_us,
    ipa_uk,
    english_definition,
    vietnamese_definition,
    vietnamese_translation,
    example_sentence,
    example_sentence_vi,
    image_url,
    sort_order
)
SELECT
    id,
    'apple',
    'Noun',
    '/ˈæpəl/',
    '/ˈæpəl/',
    'A round fruit with red, green, or yellow skin and firm white flesh.',
    'Mot loai qua tron co vo do, xanh hoac vang va phan thit trang chac.',
    'qua tao',
    'She ate an apple after lunch.',
    'Co ay an mot qua tao sau bua trua.',
    'https://placehold.co/240x180/bfefff/2f356d?text=Apple',
    1
FROM sandbox_topic
UNION ALL
SELECT
    id,
    'answer',
    'Noun',
    '/ˈænsər/',
    '/ˈɑːnsə/',
    'Something that you say, write, or do as a reply to a question.',
    'Dieu ban noi, viet hoac lam de tra loi mot cau hoi.',
    'cau tra loi',
    'I know the answer to this question.',
    'Toi biet cau tra loi cho cau hoi nay.',
    'https://placehold.co/240x180/bfefff/2f356d?text=Answer',
    2
FROM sandbox_topic
UNION ALL
SELECT
    id,
    'animal',
    'Noun',
    '/ˈænɪməl/',
    '/ˈænɪməl/',
    'A living creature that can move and eat food.',
    'Mot sinh vat song co the di chuyen va an thuc an.',
    'dong vat',
    'The animal lives in the forest.',
    'Con dong vat song trong rung.',
    'https://placehold.co/240x180/bfefff/2f356d?text=Animal',
    3
FROM sandbox_topic
UNION ALL
SELECT
    id,
    'ability',
    'Noun',
    '/əˈbɪləti/',
    '/əˈbɪləti/',
    'The skill or power to do something.',
    'Ky nang hoac kha nang lam mot viec gi do.',
    'kha nang',
    'She has the ability to learn languages quickly.',
    'Co ay co kha nang hoc ngon ngu rat nhanh.',
    'https://placehold.co/240x180/bfefff/2f356d?text=Ability',
    4
FROM sandbox_topic
ON CONFLICT (topic_id, word) DO NOTHING;

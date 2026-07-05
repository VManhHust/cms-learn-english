WITH family_topic AS (
    SELECT vt.id
    FROM vocabulary_topic vt
    JOIN vocabulary_deck vd ON vd.id = vt.deck_id
    WHERE vd.slug = '1000-tu-tieng-anh-thong-dung'
      AND vt.slug = 'gia-dinh'
)
INSERT INTO vocabulary_word (
    topic_id, word, part_of_speech, ipa_us, ipa_uk,
    english_definition, vietnamese_definition, vietnamese_translation,
    example_sentence, example_sentence_vi, image_url, sort_order
)
SELECT id, 'grandparents', 'Noun', '/ˈɡrænperənts/', '/ˈɡrænpeərənts/',
       'The parents of a person''s mother or father.',
       'Cha mẹ của bố hoặc mẹ của một người.',
       'ông bà',
       'My grandparents live in the countryside.',
       'Ông bà tôi sống ở vùng nông thôn.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Grandparents', 4
FROM family_topic
UNION ALL
SELECT id, 'cousin', 'Noun', '/ˈkʌzən/', '/ˈkʌzən/',
       'A child of a person''s aunt or uncle.',
       'Con của cô, dì, chú, bác hoặc cậu của một người.',
       'anh chị em họ',
       'My cousin is the same age as me.',
       'Anh họ của tôi bằng tuổi tôi.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Cousin', 5
FROM family_topic
UNION ALL
SELECT id, 'uncle', 'Noun', '/ˈʌŋkəl/', '/ˈʌŋkəl/',
       'The brother of a person''s mother or father, or the husband of an aunt.',
       'Anh hoặc em trai của bố mẹ, hoặc chồng của cô, dì, bác.',
       'chú, bác, cậu',
       'My uncle told us a funny story.',
       'Chú tôi kể cho chúng tôi một câu chuyện vui.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Uncle', 6
FROM family_topic
UNION ALL
SELECT id, 'aunt', 'Noun', '/ænt/', '/ɑːnt/',
       'The sister of a person''s mother or father, or the wife of an uncle.',
       'Chị hoặc em gái của bố mẹ, hoặc vợ của chú, bác, cậu.',
       'cô, dì, bác gái',
       'My aunt works at a hospital.',
       'Dì tôi làm việc ở bệnh viện.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Aunt', 7
FROM family_topic
UNION ALL
SELECT id, 'nephew', 'Noun', '/ˈnefjuː/', '/ˈnefjuː/',
       'A son of a person''s brother or sister.',
       'Con trai của anh, chị hoặc em ruột của một người.',
       'cháu trai',
       'Her nephew likes drawing pictures.',
       'Cháu trai của cô ấy thích vẽ tranh.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Nephew', 8
FROM family_topic
UNION ALL
SELECT id, 'niece', 'Noun', '/niːs/', '/niːs/',
       'A daughter of a person''s brother or sister.',
       'Con gái của anh, chị hoặc em ruột của một người.',
       'cháu gái',
       'My niece is learning to read.',
       'Cháu gái tôi đang học đọc.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Niece', 9
FROM family_topic
UNION ALL
SELECT id, 'spouse', 'Noun', '/spaʊs/', '/spaʊs/',
       'A husband or wife.',
       'Chồng hoặc vợ.',
       'vợ hoặc chồng',
       'Please bring your spouse to the family dinner.',
       'Hãy đưa vợ hoặc chồng của bạn đến bữa tối gia đình.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Spouse', 10
FROM family_topic
ON CONFLICT (topic_id, word) DO NOTHING;

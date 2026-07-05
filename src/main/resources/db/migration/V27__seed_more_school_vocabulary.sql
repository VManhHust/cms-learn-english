WITH school_topic AS (
    SELECT vt.id
    FROM vocabulary_topic vt
    JOIN vocabulary_deck vd ON vd.id = vt.deck_id
    WHERE vd.slug = '1000-tu-tieng-anh-thong-dung'
      AND vt.slug = 'truong-hoc'
)
INSERT INTO vocabulary_word (
    topic_id, word, part_of_speech, ipa_us, ipa_uk,
    english_definition, vietnamese_definition, vietnamese_translation,
    example_sentence, example_sentence_vi, image_url, sort_order
)
SELECT id, 'classroom', 'Noun', '/ˈklæsruːm/', '/ˈklɑːsruːm/',
       'A room where students are taught.',
       'Phòng nơi học sinh được giảng dạy.',
       'lớp học',
       'The students entered the classroom quietly.',
       'Các học sinh bước vào lớp học một cách yên lặng.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Classroom', 3
FROM school_topic
UNION ALL
SELECT id, 'teacher', 'Noun', '/ˈtiːtʃər/', '/ˈtiːtʃə/',
       'A person whose job is to teach.',
       'Người có công việc là giảng dạy.',
       'giáo viên',
       'Our teacher explained the grammar clearly.',
       'Giáo viên của chúng tôi giải thích ngữ pháp rất rõ ràng.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Teacher', 4
FROM school_topic
UNION ALL
SELECT id, 'student', 'Noun', '/ˈstuːdənt/', '/ˈstjuːdənt/',
       'A person who is studying at a school or university.',
       'Người đang học tại trường học hoặc đại học.',
       'học sinh, sinh viên',
       'Each student must bring a notebook.',
       'Mỗi học sinh phải mang theo một quyển vở.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Student', 5
FROM school_topic
UNION ALL
SELECT id, 'homework', 'Noun', '/ˈhoʊmwɜːrk/', '/ˈhəʊmwɜːk/',
       'School work that a student does at home.',
       'Bài tập ở trường mà học sinh làm tại nhà.',
       'bài tập về nhà',
       'I finished my homework before dinner.',
       'Tôi hoàn thành bài tập về nhà trước bữa tối.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Homework', 6
FROM school_topic
UNION ALL
SELECT id, 'lesson', 'Noun', '/ˈlesən/', '/ˈlesən/',
       'A period of time when someone is taught something.',
       'Khoảng thời gian khi ai đó được dạy một nội dung nào đó.',
       'bài học, tiết học',
       'Today''s lesson is about family vocabulary.',
       'Bài học hôm nay nói về từ vựng gia đình.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Lesson', 7
FROM school_topic
UNION ALL
SELECT id, 'exam', 'Noun', '/ɪɡˈzæm/', '/ɪɡˈzæm/',
       'A formal test of knowledge or ability.',
       'Bài kiểm tra chính thức về kiến thức hoặc năng lực.',
       'kỳ thi',
       'She studied hard for the final exam.',
       'Cô ấy học chăm chỉ cho kỳ thi cuối kỳ.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Exam', 8
FROM school_topic
UNION ALL
SELECT id, 'library', 'Noun', '/ˈlaɪbreri/', '/ˈlaɪbrəri/',
       'A place where books and other materials are kept for reading or borrowing.',
       'Nơi lưu giữ sách và tài liệu để đọc hoặc mượn.',
       'thư viện',
       'We borrowed English books from the library.',
       'Chúng tôi mượn sách tiếng Anh từ thư viện.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Library', 9
FROM school_topic
UNION ALL
SELECT id, 'notebook', 'Noun', '/ˈnoʊtbʊk/', '/ˈnəʊtbʊk/',
       'A book with blank pages for writing notes.',
       'Quyển vở có các trang trống để ghi chép.',
       'quyển vở',
       'Please write the new words in your notebook.',
       'Hãy viết các từ mới vào vở của bạn.',
       'https://placehold.co/240x180/bfefff/2f356d?text=Notebook', 10
FROM school_topic
ON CONFLICT (topic_id, word) DO NOTHING;

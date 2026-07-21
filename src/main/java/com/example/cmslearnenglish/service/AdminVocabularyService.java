package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.AdminVocabularyDtos.*;
import com.example.cmslearnenglish.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminVocabularyService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String[] WORD_EXPORT_HEADERS = {
            "Bộ thẻ",
            "Nhóm (chủ đề)",
            "Từ vựng",
            "Từ loại",
            "Nghĩa tiếng Việt",
            "Định nghĩa tiếng Anh",
            "Định nghĩa tiếng Việt",
            "Câu ví dụ",
            "Dịch câu ví dụ",
            "Phiên âm Mỹ",
            "Phiên âm Anh",
            "Audio Mỹ",
            "Audio Anh",
            "Ảnh minh họa",
            "Thứ tự"
    };

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public Summary getSummary() {
        return jdbcTemplate.queryForObject("""
                SELECT (SELECT COUNT(*) FROM vocabulary_deck),
                       (SELECT COUNT(*) FROM vocabulary_topic),
                       (SELECT COUNT(*) FROM vocabulary_word),
                       (SELECT COUNT(*) FROM vocabulary_deck WHERE is_premium = TRUE)
                """, (rs, row) -> new Summary(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)));
    }

    @Transactional(readOnly = true)
    public Page<DeckResponse> getDecks(int page, int size, String sort, String order, String query, String status) {
        PageRequest pageable = pageRequest(page, size);
        String orderBy = deckSort(sort) + direction(order);
        List<Object> args = new ArrayList<>();
        String where = deckWhere(query, status, args);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vocabulary_deck d " + where, Long.class, args.toArray());
        args.add(pageable.getPageSize());
        args.add(pageable.getOffset());
        List<DeckResponse> content = jdbcTemplate.query("""
                SELECT d.*, COUNT(DISTINCT t.id)::int topic_count, COUNT(DISTINCT w.id)::int word_count
                FROM vocabulary_deck d
                LEFT JOIN vocabulary_topic t ON t.deck_id = d.id
                LEFT JOIN vocabulary_word w ON w.topic_id = t.id
                """ + where + " GROUP BY d.id ORDER BY " + orderBy + " LIMIT ? OFFSET ?", this::mapDeck, args.toArray());
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public DeckResponse getDeck(Long id) {
        List<DeckResponse> rows = jdbcTemplate.query("""
                SELECT d.*, COUNT(DISTINCT t.id)::int topic_count, COUNT(DISTINCT w.id)::int word_count
                FROM vocabulary_deck d
                LEFT JOIN vocabulary_topic t ON t.deck_id = d.id
                LEFT JOIN vocabulary_word w ON w.topic_id = t.id
                WHERE d.id = ? GROUP BY d.id
                """, this::mapDeck, id);
        return first(rows, "Vocabulary deck", id);
    }

    @Transactional
    public DeckResponse createDeck(DeckRequest request) {
        try {
            Long id = jdbcTemplate.queryForObject("""
                    INSERT INTO vocabulary_deck
                        (slug, title, category, description, cover_color, status, is_premium, learner_count, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                    """, Long.class, clean(request.slug()), clean(request.title()), clean(request.category()),
                    trim(request.description()), clean(request.coverColor()), request.status(), request.premium(),
                    request.learnerCount(), request.sortOrder());
            return getDeck(id);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("Vocabulary deck slug already exists: " + request.slug());
        }
    }

    @Transactional
    public DeckResponse updateDeck(Long id, DeckRequest request) {
        requireExists("vocabulary_deck", "Vocabulary deck", id);
        try {
            jdbcTemplate.update("""
                    UPDATE vocabulary_deck SET slug=?, title=?, category=?, description=?, cover_color=?,
                        status=?, is_premium=?, learner_count=?, sort_order=?, updated_at=NOW() WHERE id=?
                    """, clean(request.slug()), clean(request.title()), clean(request.category()), trim(request.description()),
                    clean(request.coverColor()), request.status(), request.premium(), request.learnerCount(), request.sortOrder(), id);
            return getDeck(id);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("Vocabulary deck slug already exists: " + request.slug());
        }
    }

    @Transactional
    public void deleteDeck(Long id) {
        requireExists("vocabulary_deck", "Vocabulary deck", id);
        jdbcTemplate.update("DELETE FROM vocabulary_deck WHERE id = ?", id);
    }

    @Transactional(readOnly = true)
    public Page<TopicResponse> getTopics(int page, int size, String sort, String order, String query, Long deckId) {
        PageRequest pageable = pageRequest(page, size);
        List<Object> args = new ArrayList<>();
        String where = topicWhere(query, deckId, args);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vocabulary_topic t " + where, Long.class, args.toArray());
        args.add(pageable.getPageSize());
        args.add(pageable.getOffset());
        List<TopicResponse> content = jdbcTemplate.query("""
                SELECT t.*, d.title deck_title, COUNT(w.id)::int word_count
                FROM vocabulary_topic t JOIN vocabulary_deck d ON d.id=t.deck_id
                LEFT JOIN vocabulary_word w ON w.topic_id=t.id
                """ + where + " GROUP BY t.id, d.title ORDER BY " + topicSort(sort) + direction(order) + " LIMIT ? OFFSET ?",
                this::mapTopic, args.toArray());
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public TopicResponse getTopic(Long id) {
        List<TopicResponse> rows = jdbcTemplate.query("""
                SELECT t.*, d.title deck_title, COUNT(w.id)::int word_count
                FROM vocabulary_topic t JOIN vocabulary_deck d ON d.id=t.deck_id
                LEFT JOIN vocabulary_word w ON w.topic_id=t.id WHERE t.id=?
                GROUP BY t.id, d.title
                """, this::mapTopic, id);
        return first(rows, "Vocabulary topic", id);
    }

    @Transactional
    public TopicResponse createTopic(TopicRequest request) {
        requireExists("vocabulary_deck", "Vocabulary deck", request.deckId());
        try {
            Long id = jdbcTemplate.queryForObject("""
                    INSERT INTO vocabulary_topic (deck_id, slug, title, description, thumbnail_url, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                    """, Long.class, request.deckId(), clean(request.slug()), clean(request.title()),
                    trim(request.description()), trim(request.thumbnailUrl()), request.sortOrder());
            return getTopic(id);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("Topic slug already exists in this deck: " + request.slug());
        }
    }

    @Transactional
    public TopicResponse updateTopic(Long id, TopicRequest request) {
        requireExists("vocabulary_topic", "Vocabulary topic", id);
        requireExists("vocabulary_deck", "Vocabulary deck", request.deckId());
        try {
            jdbcTemplate.update("""
                    UPDATE vocabulary_topic SET deck_id=?, slug=?, title=?, description=?, thumbnail_url=?,
                        sort_order=?, updated_at=NOW() WHERE id=?
                    """, request.deckId(), clean(request.slug()), clean(request.title()), trim(request.description()),
                    trim(request.thumbnailUrl()), request.sortOrder(), id);
            return getTopic(id);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("Topic slug already exists in this deck: " + request.slug());
        }
    }

    @Transactional
    public void deleteTopic(Long id) {
        requireExists("vocabulary_topic", "Vocabulary topic", id);
        jdbcTemplate.update("DELETE FROM vocabulary_topic WHERE id=?", id);
    }

    @Transactional(readOnly = true)
    public Page<WordResponse> getWords(int page, int size, String sort, String order, String query, Long topicId, Long deckId) {
        PageRequest pageable = pageRequest(page, size);
        List<Object> args = new ArrayList<>();
        String where = wordWhere(query, topicId, deckId, args);
        String from = " FROM vocabulary_word w JOIN vocabulary_topic t ON t.id=w.topic_id JOIN vocabulary_deck d ON d.id=t.deck_id ";
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*)" + from + where, Long.class, args.toArray());
        args.add(pageable.getPageSize());
        args.add(pageable.getOffset());
        List<WordResponse> content = jdbcTemplate.query("SELECT w.*, t.title topic_title, d.id deck_id, d.title deck_title" +
                from + where + " ORDER BY " + wordSort(sort) + direction(order) + " LIMIT ? OFFSET ?", this::mapWord, args.toArray());
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public WordResponse getWord(Long id) {
        List<WordResponse> rows = jdbcTemplate.query("""
                SELECT w.*, t.title topic_title, d.id deck_id, d.title deck_title
                FROM vocabulary_word w JOIN vocabulary_topic t ON t.id=w.topic_id
                JOIN vocabulary_deck d ON d.id=t.deck_id WHERE w.id=?
                """, this::mapWord, id);
        return first(rows, "Vocabulary word", id);
    }

    @Transactional
    public WordResponse createWord(WordRequest request) {
        requireExists("vocabulary_topic", "Vocabulary topic", request.topicId());
        try {
            Long id = jdbcTemplate.queryForObject("""
                    INSERT INTO vocabulary_word (topic_id, word, part_of_speech, ipa_us, ipa_uk,
                        audio_us_url, audio_uk_url, english_definition, vietnamese_definition,
                        vietnamese_translation, example_sentence, example_sentence_vi, image_url, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                    """, Long.class, wordArgs(request));
            return getWord(id);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("Word already exists in this topic: " + request.word());
        }
    }

    @Transactional
    public WordResponse updateWord(Long id, WordRequest request) {
        requireExists("vocabulary_word", "Vocabulary word", id);
        requireExists("vocabulary_topic", "Vocabulary topic", request.topicId());
        try {
            Object[] values = wordArgs(request);
            Object[] args = java.util.Arrays.copyOf(values, values.length + 1);
            args[values.length] = id;
            jdbcTemplate.update("""
                    UPDATE vocabulary_word SET topic_id=?, word=?, part_of_speech=?, ipa_us=?, ipa_uk=?,
                        audio_us_url=?, audio_uk_url=?, english_definition=?, vietnamese_definition=?,
                        vietnamese_translation=?, example_sentence=?, example_sentence_vi=?, image_url=?,
                        sort_order=?, updated_at=NOW() WHERE id=?
                    """, args);
            return getWord(id);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("Word already exists in this topic: " + request.word());
        }
    }

    @Transactional
    public void deleteWord(Long id) {
        requireExists("vocabulary_word", "Vocabulary word", id);
        jdbcTemplate.update("DELETE FROM vocabulary_word WHERE id=?", id);
    }

    @Transactional(readOnly = true)
    public byte[] exportWordsCsv(String query, Long topicId, Long deckId, String sort, String order) {
        List<Object> args = new ArrayList<>();
        String where = wordWhere(query, topicId, deckId, args);
        String from = " FROM vocabulary_word w JOIN vocabulary_topic t ON t.id=w.topic_id JOIN vocabulary_deck d ON d.id=t.deck_id ";
        List<WordResponse> words = jdbcTemplate.query("SELECT w.*, t.title topic_title, d.id deck_id, d.title deck_title" +
                from + where + " ORDER BY " + wordSort(sort) + direction(order), this::mapWord, args.toArray());

        StringBuilder csv = new StringBuilder("\uFEFF");
        appendCsvRow(csv, List.of(WORD_EXPORT_HEADERS));

        for (WordResponse word : words) {
            appendCsvRow(csv, java.util.Arrays.asList(
                    word.deckTitle(),
                    word.topicTitle(),
                    word.word(),
                    word.partOfSpeech(),
                    word.vietnameseTranslation(),
                    word.englishDefinition(),
                    word.vietnameseDefinition(),
                    word.exampleSentence(),
                    word.exampleSentenceVi(),
                    word.ipaUs(),
                    word.ipaUk(),
                    word.audioUsUrl(),
                    word.audioUkUrl(),
                    word.imageUrl(),
                    String.valueOf(word.sortOrder())
            ));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportWordsExcel(String query, Long topicId, Long deckId, String sort, String order) {
        List<Object> args = new ArrayList<>();
        String where = wordWhere(query, topicId, deckId, args);
        String from = " FROM vocabulary_word w JOIN vocabulary_topic t ON t.id=w.topic_id JOIN vocabulary_deck d ON d.id=t.deck_id ";
        List<WordResponse> words = jdbcTemplate.query("SELECT w.*, t.title topic_title, d.id deck_id, d.title deck_title" +
                from + where + " ORDER BY " + wordSort(sort) + direction(order), this::mapWord, args.toArray());

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Từ vựng");
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            String[] headers = {
                    "Bộ thẻ",
                    "Nhóm (chủ đề)",
                    "Từ vựng",
                    "Từ loại",
                    "Nghĩa tiếng Việt",
                    "Định nghĩa tiếng Anh",
                    "Định nghĩa tiếng Việt",
                    "Câu ví dụ",
                    "Dịch câu ví dụ",
                    "Phiên âm Mỹ",
                    "Phiên âm Anh",
                    "Audio Mỹ",
                    "Audio Anh",
                    "Ảnh minh họa",
                    "Thứ tự"
            };

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < WORD_EXPORT_HEADERS.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(WORD_EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int rowIndex = 0; rowIndex < words.size(); rowIndex++) {
                WordResponse word = words.get(rowIndex);
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex + 1);
                setCell(row, 0, word.deckTitle());
                setCell(row, 1, word.topicTitle());
                setCell(row, 2, word.word());
                setCell(row, 3, word.partOfSpeech());
                setCell(row, 4, word.vietnameseTranslation());
                setCell(row, 5, word.englishDefinition());
                setCell(row, 6, word.vietnameseDefinition());
                setCell(row, 7, word.exampleSentence());
                setCell(row, 8, word.exampleSentenceVi());
                setCell(row, 9, word.ipaUs());
                setCell(row, 10, word.ipaUk());
                setCell(row, 11, word.audioUsUrl());
                setCell(row, 12, word.audioUkUrl());
                setCell(row, 13, word.imageUrl());
                row.createCell(14).setCellValue(word.sortOrder());
            }

            for (int i = 0; i < WORD_EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not export vocabulary Excel file", exception);
        }
    }


    @Transactional
    public ImportResult importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required");
        }

        ImportCounter counter = new ImportCounter();
        Map<String, Long> deckIds = new HashMap<>();
        Map<String, Long> topicIds = new HashMap<>();
        Set<String> touchedDecks = new HashSet<>();
        Set<String> touchedTopics = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            Map<String, Integer> headers = headerMap(parseCsvLine(stripBom(headerLine)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                counter.rows++;
                List<String> row = parseCsvLine(line);
                CsvVocabularyRow csv = toVocabularyRow(headers, row);
                if (!csv.isValid()) {
                    counter.skippedRows++;
                    continue;
                }

                String deckSlug = slugify(csv.deckTitle());
                Long deckId = deckIds.get(deckSlug);
                if (deckId == null) {
                    deckId = findDeckId(deckSlug);
                    if (deckId == null) {
                        deckId = createImportedDeck(deckSlug, csv.deckTitle());
                        counter.decksCreated++;
                    } else if (touchedDecks.add(deckSlug)) {
                        updateImportedDeck(deckId, csv.deckTitle());
                        counter.decksUpdated++;
                    }
                    deckIds.put(deckSlug, deckId);
                }

                String topicSlug = slugify(csv.topicTitle());
                String topicKey = deckId + ":" + topicSlug;
                Long topicId = topicIds.get(topicKey);
                if (topicId == null) {
                    topicId = findTopicId(deckId, topicSlug);
                    if (topicId == null) {
                        topicId = createImportedTopic(deckId, topicSlug, csv.topicTitle());
                        counter.topicsCreated++;
                    } else if (touchedTopics.add(topicKey)) {
                        updateImportedTopic(topicId, csv.topicTitle());
                        counter.topicsUpdated++;
                    }
                    topicIds.put(topicKey, topicId);
                }

                if (upsertImportedWord(topicId, csv)) {
                    counter.wordsCreated++;
                } else {
                    counter.wordsUpdated++;
                }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read CSV file", ex);
        }

        return counter.toResult();
    }

    private CsvVocabularyRow toVocabularyRow(Map<String, Integer> headers, List<String> row) {
        return new CsvVocabularyRow(
                value(headers, row, "tu tieng anh", "word"),
                value(headers, row, "loai tu", "partOfSpeech"),
                value(headers, row, "nghia tieng viet", "vietnameseTranslation"),
                value(headers, row, "dinh nghia tieng anh", "englishDefinition"),
                value(headers, row, "dinh nghia tieng viet", "vietnameseDefinition"),
                value(headers, row, "vi du tieng anh", "exampleSentence"),
                value(headers, row, "vi du tieng viet", "exampleSentenceVi"),
                value(headers, row, "phat am us", "ipaUs"),
                value(headers, row, "phat am uk", "ipaUk"),
                value(headers, row, "chu de", "topic"),
                value(headers, row, "bo de", "deck"),
                value(headers, row, "link anh", "imageUrl", "image_url")
        );
    }

    private Long createImportedDeck(String slug, String title) {
        Integer sortOrder = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(sort_order), 0) + 1 FROM vocabulary_deck", Integer.class);
        return jdbcTemplate.queryForObject("""
                INSERT INTO vocabulary_deck (slug, title, category, description, cover_color, status, is_premium, learner_count, sort_order)
                VALUES (?, ?, ?, ?, ?, 'PUBLISHED', FALSE, 0, ?) RETURNING id
                """, Long.class, slug, title, "Oxford Vocabulary", "Imported from CSV", "#2f356d", sortOrder == null ? 0 : sortOrder);
    }

    private void updateImportedDeck(Long id, String title) {
        jdbcTemplate.update("UPDATE vocabulary_deck SET title=?, updated_at=NOW() WHERE id=?", title, id);
    }

    private Long createImportedTopic(Long deckId, String slug, String title) {
        Integer sortOrder = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(sort_order), 0) + 1 FROM vocabulary_topic WHERE deck_id=?", Integer.class, deckId);
        return jdbcTemplate.queryForObject("""
                INSERT INTO vocabulary_topic (deck_id, slug, title, description, thumbnail_url, sort_order)
                VALUES (?, ?, ?, ?, NULL, ?) RETURNING id
                """, Long.class, deckId, slug, title, "Imported from CSV", sortOrder == null ? 0 : sortOrder);
    }

    private void updateImportedTopic(Long id, String title) {
        jdbcTemplate.update("UPDATE vocabulary_topic SET title=?, updated_at=NOW() WHERE id=?", title, id);
    }

    private boolean upsertImportedWord(Long topicId, CsvVocabularyRow csv) {
        Long wordId = findWordId(csv.word());
        String imageUrl = importedImageUrl(csv);
        if (wordId == null) {
            Integer sortOrder = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(sort_order), 0) + 1 FROM vocabulary_word WHERE topic_id=?", Integer.class, topicId);
            jdbcTemplate.update("""
                    INSERT INTO vocabulary_word (topic_id, word, part_of_speech, ipa_us, ipa_uk, audio_us_url, audio_uk_url,
                        english_definition, vietnamese_definition, vietnamese_translation, example_sentence, example_sentence_vi, image_url, sort_order)
                    VALUES (?, ?, ?, ?, ?, NULL, NULL, ?, ?, ?, ?, ?, ?, ?)
                    """, topicId, truncate(csv.word(), 160), truncate(csv.partOfSpeech(), 80), truncate(csv.ipaUs(), 120),
                    truncate(csv.ipaUk(), 120), csv.englishDefinition(), csv.vietnameseDefinition(), truncate(csv.vietnameseTranslation(), 255),
                    trim(csv.exampleSentence()), trim(csv.exampleSentenceVi()), imageUrl, sortOrder == null ? 0 : sortOrder);
            return true;
        }

        jdbcTemplate.update("""
                UPDATE vocabulary_word SET topic_id=?, part_of_speech=?, ipa_us=?, ipa_uk=?, english_definition=?, vietnamese_definition=?,
                    vietnamese_translation=?, example_sentence=?, example_sentence_vi=?, image_url=?, updated_at=NOW() WHERE id=?
                """, topicId, truncate(csv.partOfSpeech(), 80), truncate(csv.ipaUs(), 120), truncate(csv.ipaUk(), 120), csv.englishDefinition(),
                csv.vietnameseDefinition(), truncate(csv.vietnameseTranslation(), 255), trim(csv.exampleSentence()), trim(csv.exampleSentenceVi()), imageUrl, wordId);
        return false;
    }

    private String importedImageUrl(CsvVocabularyRow csv) {
        String imageUrl = trim(csv.imageUrl());
        if (imageUrl != null) {
            return imageUrl;
        }
        return "https://placehold.co/240x180/bfefff/2f356d?text="
                + URLEncoder.encode(csv.word(), StandardCharsets.UTF_8);
    }
    private Long findDeckId(String slug) {
        return findLong("SELECT id FROM vocabulary_deck WHERE slug=?", slug);
    }

    private Long findTopicId(Long deckId, String slug) {
        return findLong("SELECT id FROM vocabulary_topic WHERE deck_id=? AND slug=?", deckId, slug);
    }

    private Long findWordId(String word) {
        return findLong("SELECT id FROM vocabulary_word WHERE LOWER(word)=LOWER(?)", word);
    }

    private Long findLong(String sql, Object... args) {
        List<Long> rows = jdbcTemplate.query(sql, (rs, row) -> rs.getLong(1), args);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private Map<String, Integer> headerMap(List<String> headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            map.put(normalizeHeader(headers.get(i)), i);
        }
        return map;
    }

    private String value(Map<String, Integer> headers, List<String> row, String... aliases) {
        for (String alias : aliases) {
            Integer index = headers.get(normalizeHeader(alias));
            if (index != null && index < row.size()) {
                return trim(row.get(index));
            }
        }
        return null;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private String stripBom(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF' ? value.substring(1) : value;
    }

    private String normalizeHeader(String value) {
        String normalizedValue = stripBom(value == null ? "" : value)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('\u0111', 'd')
                .replace('\u00f0', 'd');
        return Normalizer.normalize(normalizedValue, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize((value == null ? "" : value).replace('\u0111', 'd').replace('\u00f0', 'd'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (normalized.isBlank()) {
            normalized = "imported-item";
        }
        return truncate(normalized, 120);
    }

    private String truncate(String value, int maxLength) {
        String trimmed = trim(value);
        if (trimmed == null || trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private record CsvVocabularyRow(
            String word,
            String partOfSpeech,
            String vietnameseTranslation,
            String englishDefinition,
            String vietnameseDefinition,
            String exampleSentence,
            String exampleSentenceVi,
            String ipaUs,
            String ipaUk,
            String topicTitle,
            String deckTitle,
            String imageUrl
    ) {
        boolean isValid() {
            return word != null && partOfSpeech != null && vietnameseTranslation != null
                    && englishDefinition != null && vietnameseDefinition != null
                    && topicTitle != null && deckTitle != null;
        }
    }

    private static class ImportCounter {
        int rows;
        int decksCreated;
        int decksUpdated;
        int topicsCreated;
        int topicsUpdated;
        int wordsCreated;
        int wordsUpdated;
        int skippedRows;

        ImportResult toResult() {
            return new ImportResult(rows, decksCreated, decksUpdated, topicsCreated, topicsUpdated, wordsCreated, wordsUpdated, skippedRows);
        }
    }
    private Object[] wordArgs(WordRequest r) {
        return new Object[]{r.topicId(), clean(r.word()), clean(r.partOfSpeech()), trim(r.ipaUs()), trim(r.ipaUk()),
                trim(r.audioUsUrl()), trim(r.audioUkUrl()), clean(r.englishDefinition()), clean(r.vietnameseDefinition()),
                clean(r.vietnameseTranslation()), trim(r.exampleSentence()), trim(r.exampleSentenceVi()), trim(r.imageUrl()), r.sortOrder()};
    }

    private String deckWhere(String q, String status, List<Object> args) {
        List<String> clauses = new ArrayList<>();
        if (q != null && !q.isBlank()) { clauses.add("(LOWER(d.title) LIKE ? OR LOWER(d.category) LIKE ? OR LOWER(d.slug) LIKE ?)"); addSearch(args, q, 3); }
        if (status != null && !status.isBlank()) { clauses.add("d.status = ?"); args.add(status.toUpperCase(Locale.ROOT)); }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    private String topicWhere(String q, Long deckId, List<Object> args) {
        List<String> clauses = new ArrayList<>();
        if (q != null && !q.isBlank()) { clauses.add("(LOWER(t.title) LIKE ? OR LOWER(t.slug) LIKE ?)"); addSearch(args, q, 2); }
        if (deckId != null) { clauses.add("t.deck_id=?"); args.add(deckId); }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    private String wordWhere(String q, Long topicId, Long deckId, List<Object> args) {
        List<String> clauses = new ArrayList<>();
        if (q != null && !q.isBlank()) { clauses.add("(LOWER(w.word) LIKE ? OR LOWER(w.vietnamese_translation) LIKE ? OR LOWER(w.english_definition) LIKE ?)"); addSearch(args, q, 3); }
        if (topicId != null) { clauses.add("w.topic_id=?"); args.add(topicId); }
        if (deckId != null) { clauses.add("d.id=?"); args.add(deckId); }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    private void addSearch(List<Object> args, String query, int count) {
        String value = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        for (int i = 0; i < count; i++) args.add(value);
    }

    private void appendCsvRow(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escapeCsv(values.get(i)));
        }
        csv.append("\r\n");
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void setCell(org.apache.poi.ss.usermodel.Row row, int index, String value) {
        row.createCell(index).setCellValue(value == null ? "" : value);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    private String direction(String order) { return "ASC".equalsIgnoreCase(order) ? " ASC" : " DESC"; }
    private String deckSort(String sort) { return switch (sort) { case "title" -> "d.title"; case "category" -> "d.category"; case "sortOrder" -> "d.sort_order"; case "updatedAt" -> "d.updated_at"; default -> "d.id"; }; }
    private String topicSort(String sort) { return switch (sort) { case "title" -> "t.title"; case "sortOrder" -> "t.sort_order"; case "updatedAt" -> "t.updated_at"; default -> "t.id"; }; }
    private String wordSort(String sort) { return switch (sort) { case "word" -> "w.word"; case "partOfSpeech" -> "w.part_of_speech"; case "sortOrder" -> "w.sort_order"; case "updatedAt" -> "w.updated_at"; default -> "w.id"; }; }

    private void requireExists(String table, String label, Long id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id=?", Integer.class, id);
        if (count == null || count == 0) throw new ResourceNotFoundException(label + " not found: " + id);
    }

    private <T> T first(List<T> rows, String label, Long id) {
        if (rows.isEmpty()) throw new ResourceNotFoundException(label + " not found: " + id);
        return rows.getFirst();
    }

    private String clean(String value) { return value.trim(); }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private DeckResponse mapDeck(ResultSet rs, int row) throws SQLException {
        return new DeckResponse(rs.getLong("id"), rs.getString("slug"), rs.getString("title"), rs.getString("category"),
                rs.getString("description"), rs.getString("cover_color"), rs.getString("status"), rs.getBoolean("is_premium"),
                rs.getInt("learner_count"), rs.getInt("sort_order"), rs.getInt("topic_count"), rs.getInt("word_count"),
                rs.getObject("created_at", java.time.OffsetDateTime.class), rs.getObject("updated_at", java.time.OffsetDateTime.class));
    }

    private TopicResponse mapTopic(ResultSet rs, int row) throws SQLException {
        return new TopicResponse(rs.getLong("id"), rs.getLong("deck_id"), rs.getString("deck_title"), rs.getString("slug"),
                rs.getString("title"), rs.getString("description"), rs.getString("thumbnail_url"), rs.getInt("sort_order"),
                rs.getInt("word_count"), rs.getObject("created_at", java.time.OffsetDateTime.class), rs.getObject("updated_at", java.time.OffsetDateTime.class));
    }

    private WordResponse mapWord(ResultSet rs, int row) throws SQLException {
        return new WordResponse(rs.getLong("id"), rs.getLong("topic_id"), rs.getString("topic_title"), rs.getLong("deck_id"),
                rs.getString("deck_title"), rs.getString("word"), rs.getString("part_of_speech"), rs.getString("ipa_us"),
                rs.getString("ipa_uk"), rs.getString("audio_us_url"), rs.getString("audio_uk_url"), rs.getString("english_definition"),
                rs.getString("vietnamese_definition"), rs.getString("vietnamese_translation"), rs.getString("example_sentence"),
                rs.getString("example_sentence_vi"), rs.getString("image_url"), rs.getInt("sort_order"),
                rs.getObject("created_at", java.time.OffsetDateTime.class), rs.getObject("updated_at", java.time.OffsetDateTime.class));
    }
}



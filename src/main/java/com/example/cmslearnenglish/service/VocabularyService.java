package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.VocabularyDeckDetailResponse;
import com.example.cmslearnenglish.dto.VocabularyDeckDetailResponse.DeckDetailDto;
import com.example.cmslearnenglish.dto.VocabularyDeckDetailResponse.TopicProgressDto;
import com.example.cmslearnenglish.dto.VocabularyDeckDetailResponse.WordCardDto;
import com.example.cmslearnenglish.dto.VocabularyDecksResponse;
import com.example.cmslearnenglish.dto.VocabularyDecksResponse.VocabularyDeckCardDto;
import com.example.cmslearnenglish.dto.VocabularyDecksResponse.VocabularyDeckCategoryDto;
import com.example.cmslearnenglish.dto.VocabularyResponse;
import com.example.cmslearnenglish.dto.VocabularyQuizOptionResponse;
import com.example.cmslearnenglish.dto.VocabularyReviewTopicResponse;
import com.example.cmslearnenglish.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class VocabularyService {

    private final JdbcTemplate jdbcTemplate;
    private final StreakService streakService;

    public VocabularyResponse getVocabularyData(Long userId) {
        VocabularyResponse response = jdbcTemplate.queryForObject(
            """
            SELECT
                COUNT(DISTINCT w.id)::int AS total_words,
                COUNT(DISTINCT CASE
                    WHEN p.status = 'MASTERED' THEN w.id
                END)::int AS mastered,
                COUNT(DISTINCT CASE
                    WHEN p.status = 'NOT_MASTERED' THEN w.id
                END)::int AS not_mastered,
                COUNT(DISTINCT CASE
                    WHEN p.status = 'NOT_MASTERED'
                      OR (p.status = 'MASTERED' AND p.review_completed = FALSE AND p.next_review_at <= NOW())
                    THEN w.id
                END)::int AS due_reviews,
                COALESCE(SUM(p.review_count), 0)::int AS total_reviews
            FROM vocabulary_word w
            JOIN vocabulary_topic t ON t.id = w.topic_id
            JOIN vocabulary_deck d ON d.id = t.deck_id AND d.status = 'PUBLISHED'
            LEFT JOIN user_vocabulary_word_progress p
                ON p.word_id = w.id AND p.user_id = ?
            """,
            (rs, rowNum) -> new VocabularyResponse(
                rs.getInt("total_words"),
                rs.getInt("mastered"),
                rs.getInt("not_mastered"),
                rs.getInt("due_reviews"),
                rs.getInt("total_reviews")
            ),
            userId
        );

        List<VocabularyResponse.DailyActivity> dailyActivity = jdbcTemplate.query(
            """
            SELECT
                p.updated_at::date::text AS activity_date,
                COUNT(DISTINCT p.word_id)::int AS activity_count
            FROM user_vocabulary_word_progress p
            JOIN vocabulary_word w ON w.id = p.word_id
            JOIN vocabulary_topic t ON t.id = w.topic_id
            JOIN vocabulary_deck d ON d.id = t.deck_id AND d.status = 'PUBLISHED'
            WHERE p.user_id = ?
              AND p.updated_at >= CURRENT_DATE - INTERVAL '41 days'
            GROUP BY p.updated_at::date
            ORDER BY p.updated_at::date
            """,
            (rs, rowNum) -> new VocabularyResponse.DailyActivity(
                rs.getString("activity_date"),
                rs.getInt("activity_count")
            ),
            userId
        );
        if (response != null) {
            response.setDailyActivity(dailyActivity);
        }
        return response;
    }

    public VocabularyDecksResponse getDecks(Long userId) {
        List<VocabularyDeckCardDto> decks = jdbcTemplate.query(
            """
            SELECT
                d.id, d.slug, d.title, d.category, d.description, d.cover_color, d.is_premium,
                d.learner_count,
                COUNT(DISTINCT t.id)::int AS topic_count,
                COUNT(DISTINCT w.id)::int AS word_count,
                COUNT(DISTINCT CASE WHEN p.word_id IS NOT NULL THEN w.id END)::int AS learned_words
            FROM vocabulary_deck d
            LEFT JOIN vocabulary_topic t ON t.deck_id = d.id
            LEFT JOIN vocabulary_word w ON w.topic_id = t.id
            LEFT JOIN user_vocabulary_word_progress p ON p.word_id = w.id AND p.user_id = ?
            WHERE d.status = 'PUBLISHED'
            GROUP BY d.id, d.slug, d.title, d.category, d.description, d.cover_color, d.is_premium, d.learner_count, d.sort_order
            ORDER BY d.category, d.sort_order, d.id
            """,
            (rs, rowNum) -> {
                int wordCount = rs.getInt("word_count");
                int learnedWords = rs.getInt("learned_words");
                return new VocabularyDeckCardDto(
                    rs.getLong("id"),
                    rs.getString("slug"),
                    rs.getString("title"),
                    rs.getString("category"),
                    rs.getString("description"),
                    rs.getString("cover_color"),
                    rs.getBoolean("is_premium"),
                    rs.getInt("topic_count"),
                    wordCount,
                    rs.getInt("learner_count"),
                    learnedWords,
                    percentage(learnedWords, wordCount),
                    learnedWords == wordCount && wordCount > 0 ? "Hoàn thành" : learnedWords > 0 ? "Đang học" : "Bắt đầu"
                );
            },
            userId
        );

        Map<String, List<VocabularyDeckCardDto>> grouped = new LinkedHashMap<>();
        for (VocabularyDeckCardDto deck : decks) {
            grouped.computeIfAbsent(deck.category(), ignored -> new ArrayList<>()).add(deck);
        }

        List<VocabularyDeckCategoryDto> categories = grouped.entrySet().stream()
            .map(entry -> new VocabularyDeckCategoryDto(entry.getKey(), entry.getValue().size(), entry.getValue()))
            .toList();

        return new VocabularyDecksResponse(decks.size(), categories);
    }

    public VocabularyDeckDetailResponse getDeckDetail(Long userId, Long deckId, Long topicId) {
        return getDeckDetail(userId, deckId, topicId, null);
    }

    public VocabularyDeckDetailResponse getDeckDetail(Long userId, Long deckId, Long topicId, Integer cardNumber) {
        DeckDetailDto deck = findDeck(deckId);
        List<TopicProgressDto> topics = findTopics(userId, deck.id());
        if (topics.isEmpty()) {
            return new VocabularyDeckDetailResponse(deck, topics, null, null, 0, 0, 0, 0, 0);
        }

        TopicProgressDto activeTopic = selectActiveTopic(topics, topicId);
        int resolvedCardNumber = cardNumber == null
            ? Math.min(activeTopic.currentWordIndex() + 1, activeTopic.totalWords())
            : Math.max(1, Math.min(cardNumber, activeTopic.totalWords()));
        WordCardDto currentCard = cardNumber == null
            ? findCurrentCard(userId, activeTopic.id())
            : findCardAtPosition(userId, activeTopic.id(), resolvedCardNumber - 1);
        resolvedCardNumber = currentCard == null
            ? (activeTopic.completed() ? activeTopic.totalWords() : 0)
            : resolvedCardNumber;
        int totalDeckWords = topics.stream().mapToInt(TopicProgressDto::totalWords).sum();
        int learnedDeckWords = topics.stream().mapToInt(TopicProgressDto::learnedWords).sum();

        return new VocabularyDeckDetailResponse(
            deck,
            topics,
            activeTopic,
            currentCard,
            resolvedCardNumber,
            activeTopic.totalWords(),
            totalDeckWords,
            learnedDeckWords,
            percentage(learnedDeckWords, totalDeckWords)
        );
    }

    @Transactional
    public VocabularyDeckDetailResponse reviewWord(Long userId, Long wordId, String rating) {
        ReviewContext context = findReviewContext(wordId);
        String normalizedRating = normalizeRating(rating);
        boolean correct = "MASTERED".equals(normalizedRating);

        if (!List.of("NOT_MASTERED", "MASTERED").contains(normalizedRating)) {
            throw new IllegalArgumentException("Rating không hợp lệ");
        }

        WordProgress currentProgress = findWordProgress(userId, wordId);
        ReviewSchedule schedule = reviewSchedule(normalizedRating, currentProgress);

        jdbcTemplate.update(
            """
            INSERT INTO user_vocabulary_word_progress
                (user_id, word_id, status, last_rating, review_count, correct_count, ease_factor,
                 next_review_at, mastered_review_stage, review_completed, learned_at, updated_at)
            VALUES (?, ?, ?, ?, 1, ?, ?, ?, ?, ?, CASE WHEN ? THEN NOW() ELSE NULL END, NOW())
            ON CONFLICT (user_id, word_id) DO UPDATE SET
                status = EXCLUDED.status,
                last_rating = EXCLUDED.last_rating,
                review_count = user_vocabulary_word_progress.review_count + 1,
                correct_count = user_vocabulary_word_progress.correct_count + EXCLUDED.correct_count,
                ease_factor = EXCLUDED.ease_factor,
                next_review_at = EXCLUDED.next_review_at,
                mastered_review_stage = EXCLUDED.mastered_review_stage,
                review_completed = EXCLUDED.review_completed,
                learned_at = CASE
                    WHEN EXCLUDED.learned_at IS NOT NULL THEN COALESCE(user_vocabulary_word_progress.learned_at, EXCLUDED.learned_at)
                    ELSE user_vocabulary_word_progress.learned_at
                END,
                updated_at = NOW()
            """,
            userId,
            wordId,
            normalizedRating,
            normalizedRating,
            correct ? 1 : 0,
            easeFactor(normalizedRating),
            schedule.nextReviewAt(),
            schedule.masteredReviewStage(),
            schedule.reviewCompleted(),
            correct
        );

        refreshTopicProgress(userId, context.topicId());
        streakService.checkIn(userId);
        return getDeckDetail(userId, context.deckId(), context.topicId());
    }

    @Transactional
    public VocabularyDeckDetailResponse resetTopicProgress(Long userId, Long topicId, boolean shuffle) {
        TopicContext context = findTopicContext(topicId);

        jdbcTemplate.update(
            """
            DELETE FROM user_vocabulary_word_progress
            WHERE user_id = ?
              AND word_id IN (SELECT id FROM vocabulary_word WHERE topic_id = ?)
            """,
            userId,
            topicId
        );
        jdbcTemplate.update(
            "DELETE FROM user_vocabulary_topic_progress WHERE user_id = ? AND topic_id = ?",
            userId,
            topicId
        );
        if (shuffle) {
            jdbcTemplate.update(
                """
                INSERT INTO user_vocabulary_topic_progress
                    (user_id, topic_id, learned_words, current_word_index, completion_percentage,
                     is_completed, shuffle_seed, updated_at)
                VALUES (?, ?, 0, 0, 0, FALSE, ?, NOW())
                """,
                userId,
                topicId,
                ThreadLocalRandom.current().nextLong()
            );
        }

        return getDeckDetail(userId, context.deckId(), context.topicId());
    }

    @Transactional
    public VocabularyDeckDetailResponse resetDeckProgress(Long userId, Long deckId) {
        findDeck(deckId);

        jdbcTemplate.update(
            """
            DELETE FROM user_vocabulary_word_progress
            WHERE user_id = ?
              AND word_id IN (
                  SELECT w.id
                  FROM vocabulary_word w
                  JOIN vocabulary_topic t ON t.id = w.topic_id
                  WHERE t.deck_id = ?
              )
            """,
            userId,
            deckId
        );
        jdbcTemplate.update(
            """
            DELETE FROM user_vocabulary_topic_progress
            WHERE user_id = ?
              AND topic_id IN (SELECT id FROM vocabulary_topic WHERE deck_id = ?)
            """,
            userId,
            deckId
        );

        return getDeckDetail(userId, deckId, null);
    }

    @Transactional
    public VocabularyDeckDetailResponse shuffleRemainingTopicWords(Long userId, Long topicId) {
        TopicContext context = findTopicContext(topicId);
        jdbcTemplate.update(
            """
            INSERT INTO user_vocabulary_topic_progress
                (user_id, topic_id, learned_words, current_word_index, completion_percentage,
                 is_completed, shuffle_seed, updated_at)
            VALUES (?, ?, 0, 0, 0, FALSE, ?, NOW())
            ON CONFLICT (user_id, topic_id) DO UPDATE SET
                shuffle_seed = EXCLUDED.shuffle_seed,
                updated_at = NOW()
            """,
            userId,
            topicId,
            ThreadLocalRandom.current().nextLong()
        );
        return getDeckDetail(userId, context.deckId(), context.topicId());
    }

    public List<VocabularyQuizOptionResponse> getQuizOptions(Long topicId, Long excludeWordId) {
        findTopicContext(topicId);
        return jdbcTemplate.query(
            """
            SELECT w.id, w.word, w.vietnamese_translation, w.english_definition
            FROM vocabulary_word w
            JOIN vocabulary_topic t ON t.id = w.topic_id
            WHERE t.deck_id = (SELECT deck_id FROM vocabulary_topic WHERE id = ?)
              AND w.id <> ?
            ORDER BY RANDOM()
            LIMIT 3
            """,
            (rs, rowNum) -> new VocabularyQuizOptionResponse(
                rs.getLong("id"),
                rs.getString("word"),
                rs.getString("vietnamese_translation"),
                rs.getString("english_definition")
            ),
            topicId,
            excludeWordId
        );
    }

    public List<VocabularyReviewTopicResponse> getReviewTopics(Long userId) {
        return jdbcTemplate.query(
            """
            SELECT t.id, t.slug, t.title, d.title AS deck_title, COUNT(w.id)::int AS review_word_count
            FROM user_vocabulary_word_progress p
            JOIN vocabulary_word w ON w.id = p.word_id
            JOIN vocabulary_topic t ON t.id = w.topic_id
            JOIN vocabulary_deck d ON d.id = t.deck_id
            WHERE p.user_id = ?
              AND (
                  p.status = 'NOT_MASTERED'
                  OR (p.status = 'MASTERED' AND p.review_completed = FALSE AND p.next_review_at <= NOW())
              )
              AND d.status = 'PUBLISHED'
            GROUP BY t.id, t.slug, t.title, t.sort_order, d.id, d.title, d.sort_order
            ORDER BY d.sort_order, d.id, t.sort_order, t.id
            """,
            (rs, rowNum) -> new VocabularyReviewTopicResponse(
                rs.getLong("id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("deck_title"),
                rs.getInt("review_word_count")
            ),
            userId
        );
    }

    public List<WordCardDto> getReviewWords(Long userId, Long topicId) {
        return jdbcTemplate.query(
            """
            SELECT
                w.id, w.word, w.part_of_speech, w.ipa_us, w.ipa_uk, w.audio_us_url, w.audio_uk_url,
                w.english_definition, w.vietnamese_definition, w.vietnamese_translation,
                w.example_sentence, w.example_sentence_vi, w.image_url, w.sort_order,
                p.status AS learning_status
            FROM user_vocabulary_word_progress p
            JOIN vocabulary_word w ON w.id = p.word_id
            JOIN vocabulary_topic t ON t.id = w.topic_id
            JOIN vocabulary_deck d ON d.id = t.deck_id
            WHERE p.user_id = ?
              AND (
                  p.status = 'NOT_MASTERED'
                  OR (p.status = 'MASTERED' AND p.review_completed = FALSE AND p.next_review_at <= NOW())
              )
              AND d.status = 'PUBLISHED'
              AND (CAST(? AS BIGINT) IS NULL OR t.id = ?)
            ORDER BY p.updated_at, w.id
            """,
            this::mapWordCard,
            userId,
            topicId,
            topicId
        );
    }

    public List<WordCardDto> getWords(Long userId) {
        return jdbcTemplate.query(
            """
            SELECT
                w.id, w.word, w.part_of_speech, w.ipa_us, w.ipa_uk, w.audio_us_url, w.audio_uk_url,
                w.english_definition, w.vietnamese_definition, w.vietnamese_translation,
                w.example_sentence, w.example_sentence_vi, w.image_url, w.sort_order,
                COALESCE(p.status, 'UNLEARNED') AS learning_status
            FROM vocabulary_word w
            JOIN vocabulary_topic t ON t.id = w.topic_id
            JOIN vocabulary_deck d ON d.id = t.deck_id
            LEFT JOIN user_vocabulary_word_progress p ON p.word_id = w.id AND p.user_id = ?
            WHERE d.status = 'PUBLISHED'
            ORDER BY d.sort_order, d.id, t.sort_order, t.id, w.sort_order, w.id
            """,
            this::mapWordCard,
            userId
        );
    }

    public List<WordCardDto> getTopicWords(Long userId, Long topicId) {
        findTopicContext(topicId);
        return jdbcTemplate.query(
            """
            SELECT
                w.id, w.word, w.part_of_speech, w.ipa_us, w.ipa_uk, w.audio_us_url, w.audio_uk_url,
                w.english_definition, w.vietnamese_definition, w.vietnamese_translation,
                w.example_sentence, w.example_sentence_vi, w.image_url, w.sort_order,
                COALESCE(p.status, 'UNLEARNED') AS learning_status
            FROM vocabulary_word w
            JOIN vocabulary_topic t ON t.id = w.topic_id
            JOIN vocabulary_deck d ON d.id = t.deck_id
            LEFT JOIN user_vocabulary_word_progress p ON p.word_id = w.id AND p.user_id = ?
            WHERE t.id = ? AND d.status = 'PUBLISHED'
            ORDER BY w.sort_order, w.id
            """,
            this::mapWordCard,
            userId,
            topicId
        );
    }

    public List<VocabularyQuizOptionResponse> getReviewQuizOptions(Long excludeWordId, Long topicId) {
        return jdbcTemplate.query(
            """
            SELECT w.id, w.word, w.vietnamese_translation, w.english_definition
            FROM vocabulary_word w
            JOIN vocabulary_topic t ON t.id = w.topic_id
            JOIN vocabulary_deck d ON d.id = t.deck_id
            WHERE d.status = 'PUBLISHED'
              AND w.id <> ?
              AND (CAST(? AS BIGINT) IS NULL OR t.id = ?)
            ORDER BY RANDOM()
            LIMIT 3
            """,
            (rs, rowNum) -> new VocabularyQuizOptionResponse(
                rs.getLong("id"),
                rs.getString("word"),
                rs.getString("vietnamese_translation"),
                rs.getString("english_definition")
            ),
            excludeWordId,
            topicId,
            topicId
        );
    }

    private DeckDetailDto findDeck(Long deckId) {
        List<DeckDetailDto> decks = jdbcTemplate.query(
            """
            SELECT id, slug, title, category, description, cover_color, is_premium
            FROM vocabulary_deck
            WHERE id = ? AND status = 'PUBLISHED'
            """,
            (rs, rowNum) -> new DeckDetailDto(
                rs.getLong("id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getString("cover_color"),
                rs.getBoolean("is_premium")
            ),
            deckId
        );
        if (decks.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy bộ từ vựng");
        }
        return decks.getFirst();
    }

    private List<TopicProgressDto> findTopics(Long userId, Long deckId) {
        return jdbcTemplate.query(
            """
            SELECT
                t.id, t.slug, t.title, t.description, t.thumbnail_url, t.sort_order,
                COUNT(w.id)::int AS total_words,
                COUNT(wp.word_id)::int AS learned_words,
                COUNT(CASE WHEN wp.status = 'MASTERED' THEN 1 END)::int AS mastered_words,
                COUNT(wp.word_id)::int AS current_word_index,
                COALESCE(ROUND(100.0 * COUNT(wp.word_id) / NULLIF(COUNT(w.id), 0)), 0)::int AS completion_percentage,
                COUNT(w.id) > 0 AND COUNT(wp.word_id) = COUNT(w.id) AS is_completed
            FROM vocabulary_topic t
            LEFT JOIN vocabulary_word w ON w.topic_id = t.id
            LEFT JOIN user_vocabulary_word_progress wp ON wp.word_id = w.id AND wp.user_id = ?
            WHERE t.deck_id = ?
            GROUP BY t.id, t.slug, t.title, t.description, t.thumbnail_url, t.sort_order
            ORDER BY t.sort_order, t.id
            """,
            (rs, rowNum) -> new TopicProgressDto(
                rs.getLong("id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("thumbnail_url"),
                rs.getInt("sort_order"),
                rs.getInt("total_words"),
                rs.getInt("learned_words"),
                rs.getInt("mastered_words"),
                rs.getInt("current_word_index"),
                rs.getInt("completion_percentage"),
                rs.getBoolean("is_completed")
            ),
            userId,
            deckId
        );
    }

    private WordCardDto findCurrentCard(Long userId, Long topicId) {
        List<WordCardDto> cards = jdbcTemplate.query(
            """
            SELECT
                w.id, w.word, w.part_of_speech, w.ipa_us, w.ipa_uk, w.audio_us_url, w.audio_uk_url,
                w.english_definition, w.vietnamese_definition, w.vietnamese_translation,
                w.example_sentence, w.example_sentence_vi, w.image_url, w.sort_order,
                COALESCE(p.status, 'NOT_MASTERED') AS learning_status
            FROM vocabulary_word w
            LEFT JOIN user_vocabulary_word_progress p ON p.word_id = w.id AND p.user_id = ?
            LEFT JOIN user_vocabulary_topic_progress tp ON tp.topic_id = w.topic_id AND tp.user_id = ?
            WHERE w.topic_id = ? AND p.word_id IS NULL
            ORDER BY
                CASE
                    WHEN tp.shuffle_seed IS NULL THEN w.sort_order::bigint
                    ELSE hashtextextended(w.id::text, tp.shuffle_seed)
                END,
                w.id
            LIMIT 1
            """,
            this::mapWordCard,
            userId,
            userId,
            topicId
        );
        return cards.isEmpty() ? null : cards.getFirst();
    }

    private WordCardDto findCardAtPosition(Long userId, Long topicId, int cardIndex) {
        List<WordCardDto> cards = jdbcTemplate.query(
            """
            SELECT
                w.id, w.word, w.part_of_speech, w.ipa_us, w.ipa_uk, w.audio_us_url, w.audio_uk_url,
                w.english_definition, w.vietnamese_definition, w.vietnamese_translation,
                w.example_sentence, w.example_sentence_vi, w.image_url, w.sort_order,
                COALESCE(p.status, 'NOT_MASTERED') AS learning_status
            FROM vocabulary_word w
            LEFT JOIN user_vocabulary_word_progress p ON p.word_id = w.id AND p.user_id = ?
            LEFT JOIN user_vocabulary_topic_progress tp ON tp.topic_id = w.topic_id AND tp.user_id = ?
            WHERE w.topic_id = ?
            ORDER BY
                CASE
                    WHEN tp.shuffle_seed IS NOT NULL AND p.word_id IS NULL THEN 1
                    ELSE 0
                END,
                CASE
                    WHEN tp.shuffle_seed IS NULL THEN w.sort_order::bigint
                    WHEN p.word_id IS NOT NULL THEN (EXTRACT(EPOCH FROM p.updated_at) * 1000000)::bigint
                    ELSE hashtextextended(w.id::text, tp.shuffle_seed)
                END,
                w.id
            LIMIT 1 OFFSET ?
            """,
            this::mapWordCard,
            userId,
            userId,
            topicId,
            Math.max(cardIndex, 0)
        );
        return cards.isEmpty() ? null : cards.getFirst();
    }

    private TopicProgressDto selectActiveTopic(List<TopicProgressDto> topics, Long topicId) {
        if (topicId != null) {
            return topics.stream()
                .filter(topic -> topic.id().equals(topicId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ đề từ vựng"));
        }

        return topics.stream()
            .filter(topic -> !topic.completed())
            .findFirst()
            .orElse(topics.getFirst());
    }

    private ReviewContext findReviewContext(Long wordId) {
        List<ReviewContext> contexts = jdbcTemplate.query(
            """
            SELECT d.id AS deck_id, t.slug AS topic_slug, t.id AS topic_id
            FROM vocabulary_word w
            JOIN vocabulary_topic t ON t.id = w.topic_id
            JOIN vocabulary_deck d ON d.id = t.deck_id
            WHERE w.id = ?
            """,
            (rs, rowNum) -> new ReviewContext(
                rs.getLong("deck_id"),
                rs.getString("topic_slug"),
                rs.getLong("topic_id")
            ),
            wordId
        );
        if (contexts.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy thẻ từ vựng");
        }
        return contexts.getFirst();
    }

    private WordProgress findWordProgress(Long userId, Long wordId) {
        List<WordProgress> progresses = jdbcTemplate.query(
            """
            SELECT status, mastered_review_stage, review_completed
            FROM user_vocabulary_word_progress
            WHERE user_id = ? AND word_id = ?
            """,
            (rs, rowNum) -> new WordProgress(
                rs.getString("status"),
                rs.getInt("mastered_review_stage"),
                rs.getBoolean("review_completed")
            ),
            userId,
            wordId
        );
        return progresses.isEmpty() ? null : progresses.getFirst();
    }

    private TopicContext findTopicContext(Long topicId) {
        List<TopicContext> contexts = jdbcTemplate.query(
            """
            SELECT d.id AS deck_id, t.slug AS topic_slug, t.id AS topic_id
            FROM vocabulary_topic t
            JOIN vocabulary_deck d ON d.id = t.deck_id
            WHERE t.id = ? AND d.status = 'PUBLISHED'
            """,
            (rs, rowNum) -> new TopicContext(
                rs.getLong("deck_id"),
                rs.getString("topic_slug"),
                rs.getLong("topic_id")
            ),
            topicId
        );
        if (contexts.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy nhóm từ vựng");
        }
        return contexts.getFirst();
    }

    private void refreshTopicProgress(Long userId, Long topicId) {
        jdbcTemplate.update(
            """
            INSERT INTO user_vocabulary_topic_progress
                (user_id, topic_id, learned_words, current_word_index, completion_percentage, is_completed, completed_at, updated_at)
            SELECT
                ?,
                ?,
                COUNT(p.word_id)::int,
                COUNT(p.word_id)::int,
                COALESCE(ROUND(100.0 * COUNT(p.word_id) / NULLIF(COUNT(w.id), 0)), 0)::int,
                COUNT(w.id) > 0 AND COUNT(p.word_id) = COUNT(w.id),
                CASE
                    WHEN COUNT(w.id) > 0 AND COUNT(p.word_id) = COUNT(w.id)
                    THEN NOW()
                    ELSE NULL
                END,
                NOW()
            FROM vocabulary_word w
            LEFT JOIN user_vocabulary_word_progress p ON p.word_id = w.id AND p.user_id = ?
            WHERE w.topic_id = ?
            ON CONFLICT (user_id, topic_id) DO UPDATE SET
                learned_words = EXCLUDED.learned_words,
                current_word_index = EXCLUDED.current_word_index,
                completion_percentage = EXCLUDED.completion_percentage,
                is_completed = EXCLUDED.is_completed,
                completed_at = CASE WHEN EXCLUDED.is_completed THEN COALESCE(user_vocabulary_topic_progress.completed_at, EXCLUDED.completed_at) ELSE NULL END,
                updated_at = NOW()
            """,
            userId,
            topicId,
            userId,
            topicId
        );
    }

    private WordCardDto mapWordCard(ResultSet rs, int rowNum) throws SQLException {
        return new WordCardDto(
            rs.getLong("id"),
            rs.getString("word"),
            rs.getString("part_of_speech"),
            rs.getString("ipa_us"),
            rs.getString("ipa_uk"),
            rs.getString("audio_us_url"),
            rs.getString("audio_uk_url"),
            rs.getString("english_definition"),
            rs.getString("vietnamese_definition"),
            rs.getString("vietnamese_translation"),
            rs.getString("example_sentence"),
            rs.getString("example_sentence_vi"),
            rs.getString("image_url"),
            rs.getInt("sort_order"),
            rs.getString("learning_status")
        );
    }

    private String normalizeRating(String rating) {
        return rating.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private BigDecimal easeFactor(String rating) {
        return switch (rating) {
            case "NOT_MASTERED" -> BigDecimal.valueOf(1.30);
            case "MASTERED" -> BigDecimal.valueOf(3.00);
            default -> BigDecimal.valueOf(2.50);
        };
    }

    private ReviewSchedule reviewSchedule(String rating, WordProgress currentProgress) {
        OffsetDateTime now = OffsetDateTime.now();
        if ("NOT_MASTERED".equals(rating)) {
            return new ReviewSchedule(null, 0, false);
        }

        int currentStage = currentProgress != null && "MASTERED".equals(currentProgress.status())
            ? currentProgress.masteredReviewStage()
            : 0;
        int nextStage = currentStage + 1;
        if (nextStage > 5) {
            return new ReviewSchedule(null, 5, true);
        }

        OffsetDateTime nextReviewAt = switch (nextStage) {
            case 1 -> now.plusMinutes(10);
            case 2 -> now.plusHours(1);
            case 3 -> now.plusDays(1);
            case 4 -> now.plusDays(4);
            case 5 -> now.plusDays(7);
            default -> now.plusDays(1);
        };
        return new ReviewSchedule(nextReviewAt, nextStage, false);
    }

    private int percentage(int part, int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.min(100, Math.round(part * 100f / total));
    }

    private static class ReviewContext {
        private final Long deckId;
        private final String topicSlug;
        private final Long topicId;

        private ReviewContext(Long deckId, String topicSlug, Long topicId) {
            this.deckId = deckId;
            this.topicSlug = topicSlug;
            this.topicId = topicId;
        }

        private Long deckId() {
            return deckId;
        }

        private String topicSlug() {
            return topicSlug;
        }

        private Long topicId() {
            return topicId;
        }
    }

    private record WordProgress(String status, int masteredReviewStage, boolean reviewCompleted) {
    }

    private record ReviewSchedule(OffsetDateTime nextReviewAt, int masteredReviewStage, boolean reviewCompleted) {
    }

    private static class TopicContext {
        private final Long deckId;
        private final String topicSlug;
        private final Long topicId;

        private TopicContext(Long deckId, String topicSlug, Long topicId) {
            this.deckId = deckId;
            this.topicSlug = topicSlug;
            this.topicId = topicId;
        }

        private Long deckId() {
            return deckId;
        }

        private String topicSlug() {
            return topicSlug;
        }

        private Long topicId() {
            return topicId;
        }
    }
}

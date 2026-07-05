package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.LessonPreviewDto;
import com.example.cmslearnenglish.dto.TopicDto;
import com.example.cmslearnenglish.dto.TopicLessonsResponse;
import com.example.cmslearnenglish.entity.LearningExercise;
import com.example.cmslearnenglish.entity.LearningTopic;
import com.example.cmslearnenglish.entity.YoutubeExerciseExtension;
import com.example.cmslearnenglish.repository.LearningExerciseRepository;
import com.example.cmslearnenglish.repository.LearningTopicRepository;
import com.example.cmslearnenglish.repository.YoutubeExerciseExtensionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicService {

    private final LearningTopicRepository topicRepository;
    private final YoutubeExerciseExtensionRepository extensionRepository;
    private final LearningExerciseRepository exerciseRepository;
    private final com.example.cmslearnenglish.repository.ProgressRepository progressRepository;

    public List<TopicDto> getAllTopics(Long userId) {
        return topicRepository.findAll().stream()
                .map(topic -> toDto(topic, userId))
                .toList();
    }

    public TopicLessonsResponse getLessonsBySlug(String slug, int page, int size, String sortBy, Long userId) {
        log.info("getLessonsBySlug: slug={}, page={}, size={}, sortBy={}, userId={}", slug, page, size, sortBy, userId);
        try {
            long topicId;
            try {
                topicId = Long.parseLong(slug);
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found: " + slug);
            }
            LearningTopic topic = topicRepository.findById(topicId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found: " + slug));
            log.info("Found topic: id={}, name={}", topic.getId(), topic.getTopicName());
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
            PageRequest pageable = PageRequest.of(page, size, sort);
            Page<LessonPreviewDto> result = exerciseRepository.findByLearningTopicId(topic.getId(), pageable)
                    .map(exercise -> toLessonPreview(exercise, userId));
            log.info("Returning {} lessons (total={})", result.getNumberOfElements(), result.getTotalElements());
            return new TopicLessonsResponse(
                    topic.getId(),
                    topic.getTopicName(),
                    result.getTotalElements(),
                    result.getTotalPages(),
                    result.getNumber(),
                    result.getSize(),
                    result.getContent()
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in getLessonsBySlug slug={}", slug, e);
            throw e;
        }
    }

    private TopicDto toDto(LearningTopic topic, Long userId) {
        long count = extensionRepository.countByLearningTopicId(topic.getId());
        List<LessonPreviewDto> previews = exerciseRepository
                .findTopByTopicId(topic.getId(), PageRequest.of(0, 4))
                .stream()
                .map(exercise -> toLessonPreview(exercise, userId))
                .toList();
        return new TopicDto(topic.getId(), topic.getTopicName(), String.valueOf(topic.getId()),
                topic.getDescription(), null, count, previews);
    }

    private LessonPreviewDto toLessonPreview(LearningExercise exercise, Long userId) {
        YoutubeExerciseExtension ext = exercise.getYoutubeExerciseExtension();
        String youtubeId = ext != null ? ext.getVideoId() : null;
        String thumbnail = ext != null ? ext.getThumbnailUrl() : null;
        String duration = ext != null && ext.getDurationSeconds() != null
                ? formatDuration(ext.getDurationSeconds()) : null;
        String source = ext != null && ext.getYoutubeChannel() != null
                ? ext.getYoutubeChannel().getChannelName() : null;

        // Get completion percentage for this user and lesson
        Integer completionPercentage = null;
        if (userId != null) {
            completionPercentage = progressRepository
                    .findCompletionPercentageByUserIdAndLessonId(userId, exercise.getId())
                    .orElse(null);
        }

        return new LessonPreviewDto(
                exercise.getId(),
                exercise.getTitle(),
                thumbnail,
                duration,
                exercise.getVocabularyLevel() != null ? exercise.getVocabularyLevel() : "A1",
                0L,
                source,
                true,
                false,
                youtubeId,
                youtubeId != null ? "https://www.youtube.com/watch?v=" + youtubeId : null,
                completionPercentage,
                exercise.isPremium()
        );
    }

    private String formatDuration(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }
}

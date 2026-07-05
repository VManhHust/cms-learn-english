package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.LearningExerciseDto;
import com.example.cmslearnenglish.dto.SaveExerciseRequest;
import com.example.cmslearnenglish.entity.*;
import com.example.cmslearnenglish.entity.enums.LearningExerciseType;
import com.example.cmslearnenglish.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminLessonService {

    private final LearningExerciseRepository exerciseRepository;
    private final YoutubeExerciseExtensionRepository extensionRepository;
    private final YoutubeChannelRepository channelRepository;
    private final LearningTopicRepository topicRepository;
    private final YoutubeExerciseService youtubeExerciseService;

    private static final Pattern YOUTUBE_ID_PATTERN = Pattern.compile(
        "(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})"
    );

    @Transactional
    public LearningExerciseDto importLesson(Long topicId, String youtubeUrl, String title,
                                            String level, String channelYoutubeId) {
        String videoId = extractYoutubeId(youtubeUrl);
        if (videoId == null) throw new IllegalArgumentException("Invalid YouTube URL: " + youtubeUrl);

        LearningTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicId));

        YoutubeChannel channel = channelRepository.findByChannelYoutubeId(channelYoutubeId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelYoutubeId));

        LearningExercise exercise = LearningExercise.builder()
                .uuid(videoId)
                .type(LearningExerciseType.YOUTUBE_VIDEO)
                .title(title)
                .vocabularyLevel(level != null ? level : "A1")
                .moduleCount(0)
                .learningTopic(topic)
                .build();

        YoutubeExerciseExtension ext = YoutubeExerciseExtension.builder()
                .videoId(videoId)
                .thumbnailUrl("https://img.youtube.com/vi/" + videoId + "/mqdefault.jpg")
                .youtubeChannel(channel)
                .learningExercise(exercise)
                .build();

        exercise.setYoutubeExerciseExtension(ext);
        exerciseRepository.save(exercise);
        return youtubeExerciseService.toExerciseDtoPublic(exercise, ext);
    }

    @Transactional
    public List<LearningExerciseDto> batchImport(Long topicId, String channelYoutubeId,
                                                  List<SaveExerciseRequest> requests) {
        return requests.stream()
                .map(r -> importLesson(topicId, "https://www.youtube.com/watch?v=" + r.getVideoId(),
                        r.getTitle(), r.getVocabularyLevel(), channelYoutubeId))
                .toList();
    }

    @Transactional
    public void deleteLesson(Long lessonId) {
        exerciseRepository.deleteById(lessonId);
    }

    @Transactional
    public LearningExerciseDto updateLesson(Long lessonId, String title, String level) {
        LearningExercise exercise = exerciseRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonId));
        if (title != null) exercise.setTitle(title);
        if (level != null) exercise.setVocabularyLevel(level);
        exerciseRepository.save(exercise);
        return youtubeExerciseService.toExerciseDtoPublic(exercise, exercise.getYoutubeExerciseExtension());
    }

    private String extractYoutubeId(String url) {
        if (url == null) return null;
        Matcher matcher = YOUTUBE_ID_PATTERN.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }
}

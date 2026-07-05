package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.AdminTranscriptSegmentRequest;
import com.example.cmslearnenglish.dto.ExerciseModuleDto;
import com.example.cmslearnenglish.dto.SaveModuleRequest;
import com.example.cmslearnenglish.entity.ExerciseModule;
import com.example.cmslearnenglish.entity.YoutubeModuleExtension;
import com.example.cmslearnenglish.repository.ExerciseModuleRepository;
import com.example.cmslearnenglish.repository.LearningExerciseRepository;
import com.example.cmslearnenglish.repository.YoutubeExerciseExtensionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTranscriptService {

    private final YoutubeExerciseService youtubeExerciseService;
    private final LearningExerciseRepository exerciseRepository;
    private final YoutubeExerciseExtensionRepository extensionRepository;
    private final ExerciseModuleRepository moduleRepository;

    @Transactional
    public List<ExerciseModuleDto> saveTranscript(Long lessonId, List<SaveModuleRequest> requests) {
        // lessonId = LearningExercise.id, lấy videoId từ extension rồi delegate
        String videoId = extensionRepository.findByLearningExerciseId(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonId))
                .getVideoId();
        youtubeExerciseService.saveModules(videoId, requests);
        return getTranscript(lessonId);
    }

    @Transactional(readOnly = true)
    public List<ExerciseModuleDto> getTranscript(Long lessonId) {
        if (!exerciseRepository.existsById(lessonId)) {
            throw new IllegalArgumentException("Lesson not found: " + lessonId);
        }
        return youtubeExerciseService.getModules(lessonId, 0, Integer.MAX_VALUE);
    }

    @Transactional
    public List<ExerciseModuleDto> updateTranscript(
            Long lessonId,
            List<AdminTranscriptSegmentRequest> requests) {
        if (!exerciseRepository.existsById(lessonId)) {
            throw new IllegalArgumentException("Lesson not found: " + lessonId);
        }

        for (int index = 0; index < requests.size(); index++) {
            AdminTranscriptSegmentRequest request = requests.get(index);
            validateSegment(request, index);

            ExerciseModule module = moduleRepository.findById(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Transcript segment not found: " + request.getId()));
            if (!lessonId.equals(module.getLearningExercise().getId())) {
                throw new IllegalArgumentException(
                        "Transcript segment does not belong to lesson: " + request.getId());
            }

            YoutubeModuleExtension extension = module.getYoutubeModuleExtension();
            if (extension == null) {
                throw new IllegalArgumentException(
                        "YouTube transcript data not found for segment: " + request.getId());
            }

            extension.setTimeStartMs(request.getTimeStartMs());
            extension.setTimeEndMs(request.getTimeEndMs());
            extension.setCorrectAnswer(request.getContent().trim());
            extension.setVietnameseText(normalizeOptionalText(request.getVietnameseText()));
        }

        return getTranscript(lessonId);
    }

    private void validateSegment(AdminTranscriptSegmentRequest request, int index) {
        int row = index + 1;
        if (request.getId() == null) {
            throw new IllegalArgumentException("Missing segment ID at row " + row);
        }
        if (request.getTimeStartMs() == null || request.getTimeStartMs() < 0) {
            throw new IllegalArgumentException("Start time must be at least 0 at row " + row);
        }
        if (request.getTimeEndMs() == null || request.getTimeEndMs() <= request.getTimeStartMs()) {
            throw new IllegalArgumentException("End time must be greater than start time at row " + row);
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("English subtitle is required at row " + row);
        }
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional
    public void deleteTranscript(Long lessonId) {
        String videoId = extensionRepository.findByLearningExerciseId(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonId))
                .getVideoId();
        youtubeExerciseService.saveModules(videoId, List.of());
    }
}

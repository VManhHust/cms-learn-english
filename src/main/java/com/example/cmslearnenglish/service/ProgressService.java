package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.ProgressResponse;
import com.example.cmslearnenglish.dto.SaveProgressRequest;
import com.example.cmslearnenglish.dto.SegmentResult;
import com.example.cmslearnenglish.entity.enums.DictationSubmode;
import com.example.cmslearnenglish.entity.LearningProgress;
import com.example.cmslearnenglish.entity.User;
import com.example.cmslearnenglish.exception.ConcurrentUpdateException;
import com.example.cmslearnenglish.exception.ResourceNotFoundException;
import com.example.cmslearnenglish.exception.ValidationException;
import com.example.cmslearnenglish.repository.LearningExerciseRepository;
import com.example.cmslearnenglish.repository.ProgressRepository;
import com.example.cmslearnenglish.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing user learning progress.
 * Handles saving, loading, and resetting progress for dictation exercises.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProgressService {
    
    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final LearningExerciseRepository exerciseRepository;
    private final StreakService streakService;
    
    /**
     * Save or update learning progress for a user.
     * 
     * @param userId the user ID
     * @param request the progress data to save
     * @return the saved progress response
     * @throws ResourceNotFoundException if user or lesson not found
     * @throws ValidationException if data validation fails
     * @throws ConcurrentUpdateException if concurrent update conflict detected
     */
    public ProgressResponse saveProgress(Long userId, SaveProgressRequest request) {
        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // Validate lesson exists
        exerciseRepository.findById(request.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with ID: " + request.getLessonId()));
        
        // Validate segment results
        validateSegmentResults(request.getSegmentResults());
        
        // Find existing progress or create new
        LearningProgress progress = progressRepository
                .findByUserIdAndLessonIdAndSubmode(userId, request.getLessonId(), request.getSubmode())
                .orElse(LearningProgress.builder()
                        .user(user)
                        .lessonId(request.getLessonId())
                        .submode(request.getSubmode())
                        .build());
        
        // Check for concurrent update conflict
        if (progress.getId() != null && request.getLastUpdated() != null) {
            if (progress.getUpdatedAt().isAfter(request.getLastUpdated())) {
                log.warn("Concurrent update detected for user={}, lesson={}, submode={}", 
                        userId, request.getLessonId(), request.getSubmode());
                throw new ConcurrentUpdateException("Progress has been updated by another session. Please reload and try again.");
            }
        }
        
        // Convert SegmentResult map to generic map for storage
        Map<String, Object> segmentResultsMap = convertSegmentResultsToMap(request.getSegmentResults());
        
        // Update progress
        progress.setSegmentResults(segmentResultsMap);
        progress.setUserInputs(request.getUserInputs() != null ? request.getUserInputs() : new HashMap<>());
        progress.setUpdatedAt(Instant.now());
        
        // Calculate completion percentage based on segments with isGood = true (accuracy >= 80%)
        // Use totalSegments from request if provided, otherwise fall back to segmentResults.size()
        int totalSegments = (request.getTotalSegments() != null && request.getTotalSegments() > 0)
                ? request.getTotalSegments()
                : request.getSegmentResults().size();
        int goodSegments = (int) request.getSegmentResults().values().stream()
                .filter(result -> Boolean.TRUE.equals(result.getIsGood()))
                .count();
        int completionPercentage = totalSegments > 0 ? (goodSegments * 100) / totalSegments : 0;
        
        progress.setCompletionPercentage(completionPercentage);
        
        // Mark as completed if 100%
        if (completionPercentage == 100) {
            progress.setIsCompleted(true);
            if (progress.getCompletedAt() == null) {
                progress.setCompletedAt(Instant.now());
                log.info("Exercise completed for user={}, lesson={}, submode={}", 
                        userId, request.getLessonId(), request.getSubmode());
            }
        } else {
            progress.setIsCompleted(false);
            progress.setCompletedAt(null);
        }
        
        LearningProgress saved = progressRepository.save(progress);
        
        log.info("Saved progress for user={}, lesson={}, submode={}, completion={}%", 
                userId, request.getLessonId(), request.getSubmode(), completionPercentage);

        streakService.checkIn(userId);
        
        return mapToResponse(saved);
    }
    
    /**
     * Get learning progress for a user, lesson, and submode.
     * 
     * @param userId the user ID
     * @param lessonId the lesson ID
     * @param submode the dictation submode
     * @return Optional containing the progress if found
     */
    @Transactional(readOnly = true)
    public Optional<ProgressResponse> getProgress(Long userId, Long lessonId, DictationSubmode submode) {
        return progressRepository
                .findByUserIdAndLessonIdAndSubmode(userId, lessonId, submode)
                .map(this::mapToResponse);
    }
    
    /**
     * Get all completed exercises for a user.
     * 
     * @param userId the user ID
     * @return list of completed progress responses
     */
    @Transactional(readOnly = true)
    public List<ProgressResponse> getCompletedExercises(Long userId) {
        return progressRepository
                .findCompletedExercisesByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all completed exercises for a user filtered by submode.
     * 
     * @param userId the user ID
     * @param submode the dictation submode
     * @return list of completed progress responses
     */
    @Transactional(readOnly = true)
    public List<ProgressResponse> getCompletedExercises(Long userId, DictationSubmode submode) {
        return progressRepository
                .findCompletedExercisesByUserIdAndSubmode(userId, submode)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Reset learning progress for a user, lesson, and submode.
     * Clears all segment results and user inputs.
     * 
     * @param userId the user ID
     * @param lessonId the lesson ID
     * @param submode the dictation submode
     */
    public void resetProgress(Long userId, Long lessonId, DictationSubmode submode) {
        progressRepository
                .findByUserIdAndLessonIdAndSubmode(userId, lessonId, submode)
                .ifPresent(progress -> {
                    progress.setSegmentResults(new HashMap<>());
                    progress.setUserInputs(new HashMap<>());
                    progress.setCompletionPercentage(0);
                    progress.setIsCompleted(false);
                    progress.setCompletedAt(null);
                    progress.setUpdatedAt(Instant.now());
                    progressRepository.save(progress);
                    
                    log.info("Reset progress for user={}, lesson={}, submode={}", 
                            userId, lessonId, submode);
                });
    }
    
    /**
     * Validate segment results data.
     * 
     * @param results the segment results map
     * @throws ValidationException if validation fails
     */
    private void validateSegmentResults(Map<String, SegmentResult> results) {
        if (results == null) {
            throw new ValidationException("Segment results cannot be null");
        }
        
        for (Map.Entry<String, SegmentResult> entry : results.entrySet()) {
            String key = entry.getKey();
            SegmentResult result = entry.getValue();
            
            // Validate key is a valid integer
            try {
                int index = Integer.parseInt(key);
                if (index < 0) {
                    throw new ValidationException("Segment index must be non-negative: " + key);
                }
            } catch (NumberFormatException e) {
                throw new ValidationException("Segment index must be a valid integer: " + key);
            }
            
            // Validate result data
            if (result == null) {
                throw new ValidationException("Segment result cannot be null for index: " + key);
            }
            
            if (result.getAccuracy() != null && 
                (result.getAccuracy() < 0 || result.getAccuracy() > 100)) {
                throw new ValidationException("Accuracy must be between 0 and 100 for segment: " + key);
            }
        }
    }
    
    /**
     * Convert SegmentResult map to generic map for JSON storage.
     * 
     * @param segmentResults the segment results map
     * @return generic map for storage
     */
    private Map<String, Object> convertSegmentResultsToMap(Map<String, SegmentResult> segmentResults) {
        Map<String, Object> result = new HashMap<>();
        if (segmentResults != null) {
            segmentResults.forEach((key, value) -> {
                Map<String, Object> segmentMap = new HashMap<>();
                segmentMap.put("segmentIndex", value.getSegmentIndex());
                segmentMap.put("checked", value.getChecked());
                segmentMap.put("skipped", value.getSkipped());
                segmentMap.put("accuracy", value.getAccuracy());
                segmentMap.put("isGood", value.getIsGood());
                segmentMap.put("attemptCount", value.getAttemptCount());
                segmentMap.put("errorCount", value.getErrorCount());
                result.put(key, segmentMap);
            });
        }
        return result;
    }
    
    /**
     * Convert generic map from storage to SegmentResult map.
     * 
     * @param segmentResultsMap the generic map from storage
     * @return SegmentResult map
     */
    @SuppressWarnings("unchecked")
    private Map<String, SegmentResult> convertMapToSegmentResults(Map<String, Object> segmentResultsMap) {
        Map<String, SegmentResult> result = new HashMap<>();
        if (segmentResultsMap != null) {
            segmentResultsMap.forEach((key, value) -> {
                if (value instanceof Map) {
                    Map<String, Object> segmentMap = (Map<String, Object>) value;
                    SegmentResult segmentResult = SegmentResult.builder()
                            .segmentIndex(getIntegerValue(segmentMap.get("segmentIndex")))
                            .checked(getBooleanValue(segmentMap.get("checked")))
                            .skipped(getBooleanValue(segmentMap.get("skipped")))
                            .accuracy(getIntegerValue(segmentMap.get("accuracy")))
                            .isGood(getBooleanValue(segmentMap.get("isGood")))
                            .attemptCount(getIntegerValue(segmentMap.get("attemptCount")))
                            .errorCount(getIntegerValue(segmentMap.get("errorCount")))
                            .build();
                    result.put(key, segmentResult);
                }
            });
        }
        return result;
    }
    
    /**
     * Safely convert object to Integer.
     */
    private Integer getIntegerValue(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return null;
    }
    
    /**
     * Safely convert object to Boolean.
     */
    private Boolean getBooleanValue(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return null;
    }
    
    /**
     * Map LearningProgress entity to ProgressResponse DTO.
     * 
     * @param progress the learning progress entity
     * @return the progress response DTO
     */
    private ProgressResponse mapToResponse(LearningProgress progress) {
        return ProgressResponse.builder()
                .lessonId(progress.getLessonId())
                .submode(progress.getSubmode())
                .segmentResults(convertMapToSegmentResults(progress.getSegmentResults()))
                .userInputs(progress.getUserInputs())
                .completionPercentage(progress.getCompletionPercentage())
                .isCompleted(progress.getIsCompleted())
                .completedAt(progress.getCompletedAt())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
}

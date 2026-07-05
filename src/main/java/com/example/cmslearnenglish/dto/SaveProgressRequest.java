package com.example.cmslearnenglish.dto;

import com.example.cmslearnenglish.entity.enums.DictationSubmode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for saving learning progress.
 * Contains all necessary data to persist user progress for a dictation exercise.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveProgressRequest {
    
    @NotNull(message = "Lesson ID is required")
    private Long lessonId;
    
    @NotNull(message = "Submode is required")
    private DictationSubmode submode;
    
    @NotNull(message = "Segment results are required")
    private Map<String, SegmentResult> segmentResults;
    
    /**
     * Optional total number of segments in the exercise.
     * Used to calculate accurate completion percentage.
     * If not provided, falls back to segmentResults.size().
     */
    private Integer totalSegments;
    
    /**
     * Optional map of segment index to user input text.
     */
    private Map<String, String> userInputs;
    
    /**
     * Optional timestamp of the last update from the client.
     * Used for concurrent update detection.
     */
    private Instant lastUpdated;
}

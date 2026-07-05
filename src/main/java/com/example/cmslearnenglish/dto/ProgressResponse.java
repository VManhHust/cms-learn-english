package com.example.cmslearnenglish.dto;

import com.example.cmslearnenglish.entity.enums.DictationSubmode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for learning progress response.
 * Contains all progress data returned to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {
    
    private Long lessonId;
    
    private DictationSubmode submode;
    
    private Map<String, SegmentResult> segmentResults;
    
    private Map<String, String> userInputs;
    
    private Integer completionPercentage;
    
    private Boolean isCompleted;
    
    private Instant completedAt;
    
    private Instant updatedAt;
}

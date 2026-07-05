package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the result of a single segment in a dictation exercise.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentResult {
    
    /**
     * The index of the segment (0-based).
     */
    private Integer segmentIndex;
    
    /**
     * Whether the segment has been checked/completed by the user.
     */
    private Boolean checked;
    
    /**
     * Whether the segment was skipped by the user.
     */
    private Boolean skipped;
    
    /**
     * The accuracy score for the segment (0-100).
     */
    private Integer accuracy;
    
    /**
     * Whether the segment result is considered "good" (meets quality threshold).
     */
    private Boolean isGood;
    
    /**
     * Total number of attempts (checks) for this segment.
     */
    private Integer attemptCount;
    
    /**
     * Number of failed attempts (accuracy < 80%) for this segment.
     */
    private Integer errorCount;
}

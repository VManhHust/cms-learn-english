package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptResponse {
    private Long learningTopicId;
    private List<TranscriptSegmentDto> segments;
    private Integer totalSegments;
}

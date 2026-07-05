package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptSegmentDto {
    private Long id;
    private Integer startTimeMs;
    private Integer endTimeMs;
    private String text;
    private String vietnameseText;
}

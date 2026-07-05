package com.example.cmslearnenglish.dto.youtube.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptSegment {
    private Integer id;
    private Integer startMs;
    private Integer endMs;
    private String text;
}

package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonPreviewDto {
    private Long id;
    private String title;
    private String thumbnail;
    private String duration;
    private String level;
    private long viewCount;
    private String source;
    private boolean hasDictation;
    private boolean hasShadowing;
    private String youtubeId;
    private String youtubeUrl;
    private Integer completionPercentage;
    private boolean premium;
}

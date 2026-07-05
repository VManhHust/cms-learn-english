package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicDto {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String thumbnail;
    private long lessonCount;
    private List<LessonPreviewDto> previewLessons;
}

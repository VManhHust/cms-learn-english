package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicLessonsResponse {
    private Long topicId;
    private String topicName;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private List<LessonPreviewDto> content;
}

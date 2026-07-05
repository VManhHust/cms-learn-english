package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveExerciseRequest {
    private String videoId;
    private String title;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private String vocabularyLevel;
}

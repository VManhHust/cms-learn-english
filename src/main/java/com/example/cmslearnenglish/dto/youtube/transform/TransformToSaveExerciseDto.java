package com.example.cmslearnenglish.dto.youtube.transform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransformToSaveExerciseDto {
    private String videoId;
    private String title;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private String vocabularyLevel;
}

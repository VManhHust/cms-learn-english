package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportRequest {
    private Long topicId;
    private String channelYoutubeId;
    private List<SaveExerciseRequest> lessons;
}

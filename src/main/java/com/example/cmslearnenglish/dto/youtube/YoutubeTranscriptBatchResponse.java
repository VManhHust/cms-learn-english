package com.example.cmslearnenglish.dto.youtube;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeTranscriptBatchResponse {
    private List<YoutubeTranscriptResponse> results;
    private int successCount;
    private int failureCount;
}

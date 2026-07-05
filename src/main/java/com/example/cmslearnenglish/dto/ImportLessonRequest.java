package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportLessonRequest {
    private Long topicId;
    private String youtubeUrl;
    private String title;
    private String level;
    private String channelYoutubeId;
}

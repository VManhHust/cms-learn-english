package com.example.cmslearnenglish.dto.youtube;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeTranscriptResponse {
    private boolean success;
    private String videoId;
    private VideoData video;
    private ChannelData channel;
    private String errorMessage;
}

package com.example.cmslearnenglish.dto.youtube.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoMetadata {
    private String title;
    private String videoId;
    private Integer lengthSeconds;
    private String channelId;
    private String thumbnailUrl;
}

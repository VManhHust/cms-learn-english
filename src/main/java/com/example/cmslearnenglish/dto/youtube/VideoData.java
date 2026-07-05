package com.example.cmslearnenglish.dto.youtube;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoData {
    private String title;
    private String videoID;
    private Integer length;
    private String channelID;
    private String thumbnailUrl;
    private List<CaptionSegment> captions;
}

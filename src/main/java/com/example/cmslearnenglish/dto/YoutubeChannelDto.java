package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeChannelDto {
    private Long id;
    private String channelYoutubeId;
    private String channelName;
    private String channelImgUrl;
    private String channelDescription;
    private Long subscriberCount;
}

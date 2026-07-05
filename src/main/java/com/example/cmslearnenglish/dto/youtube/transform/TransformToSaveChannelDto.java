package com.example.cmslearnenglish.dto.youtube.transform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransformToSaveChannelDto {
    private String channelName;
    private String channelYoutubeId;
    private String channelImgUrl;
    private String channelDescription;
    private Long subscriberCount;
}

package com.example.cmslearnenglish.dto.youtube;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelData {
    private String channelID;
    private String channelHandle;
    private String channelName;
    private String channelDescription;
    private ChannelThumbnails channelThumbnail;
    private ChannelStatistics channelStatistics;
}

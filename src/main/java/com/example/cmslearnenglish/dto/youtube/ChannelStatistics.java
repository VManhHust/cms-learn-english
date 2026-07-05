package com.example.cmslearnenglish.dto.youtube;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelStatistics {
    private Long viewCount;
    private Long subscriberCount;
    private Boolean hiddenSubscriberCount;
    private Long videoCount;
}

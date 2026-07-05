package com.example.cmslearnenglish.dto.youtube;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelThumbnails {
    @JsonProperty("default")
    private ThumbnailInfo defaultThumbnail;
    private ThumbnailInfo medium;
    private ThumbnailInfo high;
    private ThumbnailInfo standard;
    private ThumbnailInfo maxres;
}

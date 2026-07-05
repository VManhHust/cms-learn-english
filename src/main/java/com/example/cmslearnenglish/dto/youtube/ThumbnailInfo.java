package com.example.cmslearnenglish.dto.youtube;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThumbnailInfo {
    private String url;
    private Integer width;
    private Integer height;
}

package com.example.cmslearnenglish.dto.youtube;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptionSegment {
    private Integer id;
    private Integer t_start_ms;
    private Integer t_end_ms;
    private String caption;
}

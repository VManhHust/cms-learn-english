package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveModuleRequest {
    private Integer timeStartMs;
    private Integer timeEndMs;
    private String content;
    private String vietnameseText;
}

package com.example.cmslearnenglish.dto.youtube.transform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransformToSaveModuleDto {
    private Integer timeStartMs;
    private Integer timeEndMs;
    private String content;
}

package com.example.cmslearnenglish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ImportTranscriptRequest {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_-]{11}$", message = "videoId must be an 11-character YouTube ID")
    private String videoId;
}

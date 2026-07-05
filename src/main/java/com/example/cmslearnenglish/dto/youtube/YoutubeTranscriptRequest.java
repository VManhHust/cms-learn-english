package com.example.cmslearnenglish.dto.youtube;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeTranscriptRequest {
    @NotEmpty(message = "URLs list cannot be empty")
    private List<@NotBlank(message = "URL cannot be blank") String> urls;
}

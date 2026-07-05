package com.example.cmslearnenglish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveVocabularyRequest {

    @NotBlank(message = "Word cannot be blank")
    @Size(max = 100, message = "Word must not exceed 100 characters")
    private String word;
}

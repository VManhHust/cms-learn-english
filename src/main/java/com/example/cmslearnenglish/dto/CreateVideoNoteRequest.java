package com.example.cmslearnenglish.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * DTO for creating a new video note.
 * Contains the exercise module ID and note content.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CreateVideoNoteRequest {
    @NotNull(message = "Exercise module ID is required")
    private Long exerciseModuleId;

    @NotBlank(message = "Note content cannot be empty")
    @Size(max = 5000, message = "Note content cannot exceed 5000 characters")
    private String noteContent;
}

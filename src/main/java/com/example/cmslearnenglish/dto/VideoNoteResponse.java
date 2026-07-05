package com.example.cmslearnenglish.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Instant;

/**
 * Response DTO for video notes.
 * Contains all information about a user's note on a video segment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(fluent = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class VideoNoteResponse {
    private Long id;
    private String videoTitle;
    private Long videoId;
    private String englishText;
    private String vietnameseText;
    private String noteContent;
    private Instant createdAt;
}

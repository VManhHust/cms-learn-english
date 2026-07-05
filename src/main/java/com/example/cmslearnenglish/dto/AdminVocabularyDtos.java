package com.example.cmslearnenglish.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.time.OffsetDateTime;

public final class AdminVocabularyDtos {

    private AdminVocabularyDtos() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(fluent = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class Summary {
        private long decks;
        private long topics;
        private long words;
        private long premiumDecks;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(fluent = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class DeckResponse {
        private Long id;
        private String slug;
        private String title;
        private String category;
        private String description;
        private String coverColor;
        private String status;
        private boolean premium;
        private int learnerCount;
        private int sortOrder;
        private int topicCount;
        private int wordCount;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(fluent = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class TopicResponse {
        private Long id;
        private Long deckId;
        private String deckTitle;
        private String slug;
        private String title;
        private String description;
        private String thumbnailUrl;
        private int sortOrder;
        private int wordCount;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(fluent = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class WordResponse {
        private Long id;
        private Long topicId;
        private String topicTitle;
        private Long deckId;
        private String deckTitle;
        private String word;
        private String partOfSpeech;
        private String ipaUs;
        private String ipaUk;
        private String audioUsUrl;
        private String audioUkUrl;
        private String englishDefinition;
        private String vietnameseDefinition;
        private String vietnameseTranslation;
        private String exampleSentence;
        private String exampleSentenceVi;
        private String imageUrl;
        private int sortOrder;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(fluent = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class DeckRequest {
        @NotBlank @Size(max = 120) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must use lowercase letters, numbers and hyphens")
        private String slug;
        @NotBlank @Size(max = 255)
        private String title;
        @NotBlank @Size(max = 120)
        private String category;
        private String description;
        @NotBlank @Size(max = 30)
        private String coverColor;
        @NotBlank @Pattern(regexp = "DRAFT|PUBLISHED|ARCHIVED", message = "status must be DRAFT, PUBLISHED or ARCHIVED")
        private String status;
        private boolean premium;
        @Min(0)
        private int learnerCount;
        @Min(0)
        private int sortOrder;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(fluent = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class TopicRequest {
        @NotNull
        private Long deckId;
        @NotBlank @Size(max = 120) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "slug must use lowercase letters, numbers and hyphens")
        private String slug;
        @NotBlank @Size(max = 255)
        private String title;
        private String description;
        private String thumbnailUrl;
        @Min(0)
        private int sortOrder;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(fluent = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class WordRequest {
        @NotNull
        private Long topicId;
        @NotBlank @Size(max = 160)
        private String word;
        @NotBlank @Size(max = 80)
        private String partOfSpeech;
        @Size(max = 120)
        private String ipaUs;
        @Size(max = 120)
        private String ipaUk;
        private String audioUsUrl;
        private String audioUkUrl;
        @NotBlank
        private String englishDefinition;
        @NotBlank
        private String vietnameseDefinition;
        @NotBlank @Size(max = 255)
        private String vietnameseTranslation;
        private String exampleSentence;
        private String exampleSentenceVi;
        private String imageUrl;
        @Min(0)
        private int sortOrder;
    }
}

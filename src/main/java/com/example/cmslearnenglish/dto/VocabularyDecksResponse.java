package com.example.cmslearnenglish.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class VocabularyDecksResponse {
    private int totalDecks;
    private List<VocabularyDeckCategoryDto> categories;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(fluent = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class VocabularyDeckCategoryDto {
        private String name;
        private int deckCount;
        private List<VocabularyDeckCardDto> decks;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(fluent = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class VocabularyDeckCardDto {
        private Long id;
        private String slug;
        private String title;
        private String category;
        private String description;
        private String coverColor;
        private boolean premium;
        private int topicCount;
        private int wordCount;
        private int learnerCount;
        private int learnedWords;
        private int completionPercentage;
        private String statusLabel;
    }
}

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
public class VocabularyDeckDetailResponse {
    private DeckDetailDto deck;
    private List<TopicProgressDto> topics;
    private TopicProgressDto activeTopic;
    private WordCardDto currentCard;
    private int currentCardNumber;
    private int totalCards;
    private int totalDeckWords;
    private int learnedDeckWords;
    private int deckCompletionPercentage;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(fluent = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class DeckDetailDto {
        private Long id;
        private String slug;
        private String title;
        private String category;
        private String description;
        private String coverColor;
        private boolean premium;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(fluent = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class TopicProgressDto {
        private Long id;
        private String slug;
        private String title;
        private String description;
        private String thumbnailUrl;
        private int sortOrder;
        private int totalWords;
        private int learnedWords;
        private int masteredWords;
        private int currentWordIndex;
        private int completionPercentage;
        private boolean completed;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(fluent = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class WordCardDto {
        private Long id;
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
        private String learningStatus;
    }
}

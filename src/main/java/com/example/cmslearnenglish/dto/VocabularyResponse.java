package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyResponse {
    private int totalWords;
    private int mastered;
    private int notMastered;
    private int dueReviews;
    private int totalReviews;

    private List<DailyActivity> dailyActivity;

    public VocabularyResponse(int totalWords, int mastered, int notMastered, int totalReviews) {
        this(totalWords, mastered, notMastered, notMastered, totalReviews, List.of());
    }

    public VocabularyResponse(int totalWords, int mastered, int notMastered, int dueReviews, int totalReviews) {
        this(totalWords, mastered, notMastered, dueReviews, totalReviews, List.of());
    }

    public record DailyActivity(String date, int count) {
    }
}

package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreakResponse {
    private Integer currentStreak;
    private Boolean checkedInToday;
    private Long totalCheckIns;
    private LocalDate today;
    private List<LocalDate> checkedInDates;
}

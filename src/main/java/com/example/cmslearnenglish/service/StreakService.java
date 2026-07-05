package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.StreakResponse;
import com.example.cmslearnenglish.exception.ResourceNotFoundException;
import com.example.cmslearnenglish.repository.UserRepository;
import com.example.cmslearnenglish.repository.UserStreakCheckInRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class StreakService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserStreakCheckInRepository streakRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public StreakResponse getStatus(Long userId) {
        ensureUserExists(userId);
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        return buildResponse(userId, today);
    }

    public StreakResponse checkIn(Long userId) {
        ensureUserExists(userId);
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        streakRepository.insertIfAbsent(userId, today);
        return buildResponse(userId, today);
    }

    private StreakResponse buildResponse(Long userId, LocalDate today) {
        List<LocalDate> dates = streakRepository.findCheckInDatesByUserId(userId);
        Set<LocalDate> dateSet = new HashSet<>(dates);
        boolean checkedInToday = dateSet.contains(today);

        LocalDate cursor = checkedInToday ? today : today.minusDays(1);
        int currentStreak = 0;
        while (dateSet.contains(cursor)) {
            currentStreak++;
            cursor = cursor.minusDays(1);
        }

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        List<LocalDate> weekDates = dates.stream()
                .filter(date -> !date.isBefore(weekStart) && !date.isAfter(weekEnd))
                .sorted()
                .toList();

        return StreakResponse.builder()
                .currentStreak(currentStreak)
                .checkedInToday(checkedInToday)
                .totalCheckIns(streakRepository.countByUserId(userId))
                .today(today)
                .checkedInDates(weekDates)
                .build();
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
    }
}

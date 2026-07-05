package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.entity.LearningExercise;
import com.example.cmslearnenglish.entity.User;
import com.example.cmslearnenglish.repository.LearningExerciseRepository;
import com.example.cmslearnenglish.repository.UserRepository;
import com.example.cmslearnenglish.security.JwtClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProAccessService {

    private final LearningExerciseRepository learningExerciseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public void assertCanAccessLesson(Long lessonId, JwtClaims claims) {
        LearningExercise lesson = learningExerciseRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        if (!lesson.isPremium() || isAdmin(claims)) {
            return;
        }

        assertHasActivePro(claims);
    }

    @Transactional(readOnly = true)
    public void assertCanAccessTopicTranscripts(Long topicId, JwtClaims claims) {
        boolean hasPremiumLessons = learningExerciseRepository.existsByLearningTopicIdAndPremiumTrue(topicId);
        if (!hasPremiumLessons || isAdmin(claims)) {
            return;
        }

        assertHasActivePro(claims);
    }

    private void assertHasActivePro(JwtClaims claims) {
        if (claims == null || claims.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PRO subscription required");
        }

        User user = userRepository.findById(claims.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "PRO subscription required"));

        if (!isProActive(user, Instant.now())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "PRO subscription required");
        }
    }

    private boolean isAdmin(JwtClaims claims) {
        return claims != null && "ADMIN".equalsIgnoreCase(claims.getRole());
    }

    private boolean isProActive(User user, Instant now) {
        boolean started = user.getProStartsAt() == null || !user.getProStartsAt().isAfter(now);
        return started && user.getProExpiresAt() != null && user.getProExpiresAt().isAfter(now);
    }
}

package com.example.cmslearnenglish.repository;

import com.example.cmslearnenglish.entity.enums.DictationSubmode;
import com.example.cmslearnenglish.entity.LearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for LearningProgress entity.
 * Provides methods for CRUD operations and custom queries.
 */
@Repository
public interface ProgressRepository extends JpaRepository<LearningProgress, Long> {
    
    /**
     * Find progress by user ID, lesson ID, and submode.
     * 
     * @param userId the user ID
     * @param lessonId the lesson ID
     * @param submode the dictation submode
     * @return Optional containing the progress if found
     */
    Optional<LearningProgress> findByUserIdAndLessonIdAndSubmode(
            Long userId, 
            Long lessonId, 
            DictationSubmode submode);
    
    /**
     * Find all progress records for a user.
     * 
     * @param userId the user ID
     * @return list of progress records
     */
    List<LearningProgress> findByUserId(Long userId);
    
    /**
     * Find all completed exercises for a user.
     * 
     * @param userId the user ID
     * @return list of completed progress records
     */
    @Query("SELECT p FROM LearningProgress p WHERE p.user.id = :userId AND p.isCompleted = true")
    List<LearningProgress> findCompletedExercisesByUserId(@Param("userId") Long userId);
    
    /**
     * Find all completed exercises for a user filtered by submode.
     * 
     * @param userId the user ID
     * @param submode the dictation submode
     * @return list of completed progress records
     */
    @Query("SELECT p FROM LearningProgress p WHERE p.user.id = :userId AND p.isCompleted = true AND p.submode = :submode")
    List<LearningProgress> findCompletedExercisesByUserIdAndSubmode(
            @Param("userId") Long userId, 
            @Param("submode") DictationSubmode submode);
    
    /**
     * Delete all progress records for a user and lesson.
     * 
     * @param userId the user ID
     * @param lessonId the lesson ID
     */
    @Modifying
    @Query("DELETE FROM LearningProgress p WHERE p.user.id = :userId AND p.lessonId = :lessonId")
    void deleteByUserIdAndLessonId(@Param("userId") Long userId, @Param("lessonId") Long lessonId);
    
    /**
     * Get completion percentage for a user and lesson.
     * Returns the completion_percentage from the progress record.
     * 
     * @param userId the user ID
     * @param lessonId the lesson ID
     * @return Optional containing the completion percentage if found
     */
    @Query("SELECT p.completionPercentage FROM LearningProgress p WHERE p.user.id = :userId AND p.lessonId = :lessonId ORDER BY p.completionPercentage DESC")
    Optional<Integer> findCompletionPercentageByUserIdAndLessonId(@Param("userId") Long userId, @Param("lessonId") Long lessonId);
}

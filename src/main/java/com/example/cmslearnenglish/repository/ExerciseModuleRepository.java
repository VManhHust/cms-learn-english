package com.example.cmslearnenglish.repository;

import com.example.cmslearnenglish.entity.ExerciseModule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ExerciseModuleRepository extends JpaRepository<ExerciseModule, Long> {

    @Query("""
        SELECT m FROM ExerciseModule m
        JOIN m.youtubeModuleExtension ext
        WHERE m.learningExercise.id = :exerciseId
        ORDER BY ext.timeStartMs ASC
        """)
    List<ExerciseModule> findByExerciseIdOrderByTimeStart(Long exerciseId, Pageable pageable);

    @Query("SELECT m.id FROM ExerciseModule m JOIN m.youtubeModuleExtension ext WHERE m.learningExercise.id = :exerciseId ORDER BY ext.timeStartMs ASC")
    List<Long> findIdsByExerciseId(Long exerciseId);

    @Query("SELECT m FROM ExerciseModule m JOIN FETCH m.learningExercise WHERE m.youtubeModuleExtension.id = :youtubeModuleExtensionId")
    java.util.Optional<ExerciseModule> findByYoutubeModuleExtensionId(Long youtubeModuleExtensionId);
}

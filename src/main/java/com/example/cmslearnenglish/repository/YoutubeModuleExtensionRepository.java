package com.example.cmslearnenglish.repository;

import com.example.cmslearnenglish.entity.YoutubeModuleExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface YoutubeModuleExtensionRepository extends JpaRepository<YoutubeModuleExtension, Long> {

    @Query("""
        SELECT yme FROM YoutubeModuleExtension yme
        JOIN ExerciseModule em ON em.youtubeModuleExtension.id = yme.id
        JOIN LearningExercise le ON em.learningExercise.id = le.id
        JOIN LearningTopic lt ON le.learningTopic.id = lt.id
        WHERE lt.id = :learningTopicId
        ORDER BY yme.timeStartMs ASC
        """)
    List<YoutubeModuleExtension> findByLearningTopicIdOrderByTimeStartMsAsc(@Param("learningTopicId") Long learningTopicId);
}

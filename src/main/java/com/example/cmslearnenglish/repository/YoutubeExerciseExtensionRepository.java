package com.example.cmslearnenglish.repository;

import com.example.cmslearnenglish.entity.YoutubeExerciseExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface YoutubeExerciseExtensionRepository extends JpaRepository<YoutubeExerciseExtension, Long> {
    Optional<YoutubeExerciseExtension> findByVideoId(String videoId);

    @Query("SELECT e FROM YoutubeExerciseExtension e WHERE e.youtubeChannel.id = :channelId")
    Page<YoutubeExerciseExtension> findByChannelId(Long channelId, Pageable pageable);

    Optional<YoutubeExerciseExtension> findByLearningExerciseId(Long learningExerciseId);

    @Query("SELECT COUNT(e) FROM YoutubeExerciseExtension e WHERE e.learningExercise.learningTopic.id = :topicId")
    long countByLearningTopicId(Long topicId);
}

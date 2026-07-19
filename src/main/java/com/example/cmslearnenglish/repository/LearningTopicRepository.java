package com.example.cmslearnenglish.repository;

import com.example.cmslearnenglish.entity.LearningTopic;
import com.example.cmslearnenglish.entity.enums.LearningTopicType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LearningTopicRepository extends JpaRepository<LearningTopic, Long> {
    @Query("SELECT t FROM LearningTopic t WHERE t.type = :type ORDER BY t.id ASC LIMIT 1")
    Optional<LearningTopic> findByType(LearningTopicType type);

    // Tìm theo id (slug là id của topic)
    @Query(value = "SELECT * FROM learning_topic WHERE id = :id", nativeQuery = true)
    Optional<LearningTopic> findBySlug(Long id);

    Page<LearningTopic> findByTopicNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String topicName,
            String description,
            Pageable pageable);
}

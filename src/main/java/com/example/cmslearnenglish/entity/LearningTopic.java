package com.example.cmslearnenglish.entity;

import com.example.cmslearnenglish.entity.enums.LearningTopicType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "learning_topic")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_name", nullable = false)
    private String topicName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LearningTopicType type;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "learningTopic", cascade = CascadeType.PERSIST)
    @Builder.Default
    @JsonIgnore
    private Set<LearningExercise> exercises = new HashSet<>();
}

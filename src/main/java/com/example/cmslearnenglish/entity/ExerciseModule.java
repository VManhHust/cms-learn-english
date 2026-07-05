package com.example.cmslearnenglish.entity;

import com.example.cmslearnenglish.entity.enums.ExerciseModuleType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercise_module")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseModuleType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    @JsonIgnore
    private LearningExercise learningExercise;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "youtube_module_extension_id")
    private YoutubeModuleExtension youtubeModuleExtension;
}

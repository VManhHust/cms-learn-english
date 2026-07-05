package com.example.cmslearnenglish.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exercise_module_youtube_extension")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeModuleExtension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "time_start_ms", nullable = false)
    private Integer timeStartMs;

    @Column(name = "time_end_ms", nullable = false)
    private Integer timeEndMs;

    @Column(name = "correct_answer", columnDefinition = "TEXT", nullable = false)
    private String correctAnswer;

    @Column(name = "vietnamese_text", columnDefinition = "TEXT")
    private String vietnameseText;
}

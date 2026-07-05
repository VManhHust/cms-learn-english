package com.example.cmslearnenglish.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "video_notes",
    indexes = {
        @Index(name = "idx_video_notes_user_created", columnList = "user_id, created_at DESC"),
        @Index(name = "idx_video_notes_exercise_module", columnList = "exercise_module_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private LearningTopic video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_module_id", nullable = false)
    private YoutubeModuleExtension exerciseModuleExtension;

    @Column(name = "note_content", columnDefinition = "TEXT", nullable = false)
    private String noteContent;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

package com.example.cmslearnenglish.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "learning_exercise_youtube_extension")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"learningExercise", "youtubeChannel"})
@EqualsAndHashCode(exclude = {"learningExercise", "youtubeChannel"})
public class YoutubeExerciseExtension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private String videoId;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "learning_exercise_id", nullable = false)
    @JsonManagedReference
    private LearningExercise learningExercise;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "youtube_channel_id")
    @JsonBackReference
    private YoutubeChannel youtubeChannel;
}

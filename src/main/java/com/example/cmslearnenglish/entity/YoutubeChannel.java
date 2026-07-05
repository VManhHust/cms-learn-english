package com.example.cmslearnenglish.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "youtube_channels")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "extensions")
@EqualsAndHashCode(exclude = "extensions")
public class YoutubeChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_youtube_id", unique = true, nullable = false)
    private String channelYoutubeId;

    @Column(name = "channel_name")
    private String channelName;

    @Column(name = "channel_img_url")
    private String channelImgUrl;

    @Column(name = "channel_description", columnDefinition = "TEXT")
    private String channelDescription;

    @Column(name = "channel_subscriber_count")
    private Long subscriberCount;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "youtubeChannel", cascade = CascadeType.PERSIST)
    @Builder.Default
    @JsonManagedReference
    private Set<YoutubeExerciseExtension> extensions = new HashSet<>();
}

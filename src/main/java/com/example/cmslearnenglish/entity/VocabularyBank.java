package com.example.cmslearnenglish.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "vocabulary_bank",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_vocabulary_bank_user_word",
        columnNames = {"user_id", "word"}
    ),
    indexes = {
        @Index(name = "idx_vocabulary_bank_user_added", columnList = "user_id, added_at DESC")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(name = "added_at", nullable = false)
    @Builder.Default
    private Instant addedAt = Instant.now();
}

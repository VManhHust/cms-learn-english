package com.example.cmslearnenglish.entity;

import com.example.cmslearnenglish.entity.enums.DictationSubmode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing user learning progress for dictation exercises.
 * Stores progress data including segment results, user inputs, and completion status.
 */
@Entity
@Table(
    name = "learning_progress",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_lesson_submode",
        columnNames = {"user_id", "lesson_id", "submode"}
    ),
    indexes = {
        @Index(name = "idx_learning_progress_user_lesson", columnList = "user_id, lesson_id"),
        @Index(name = "idx_learning_progress_updated_at", columnList = "updated_at"),
        @Index(name = "idx_learning_progress_user_completed", columnList = "user_id, is_completed")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;
    
    @Convert(converter = DictationSubmodeConverter.class)
    @Column(name = "submode", nullable = false, length = 20)
    private DictationSubmode submode;
    
    /**
     * Map of segment index to segment result data.
     * Stored as JSONB in PostgreSQL for flexible schema.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "segment_results", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> segmentResults = new HashMap<>();
    
    /**
     * Map of segment index to user input text.
     * Stored as JSONB in PostgreSQL.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "user_inputs", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> userInputs = new HashMap<>();
    
    @Column(name = "completion_percentage", nullable = false)
    @Builder.Default
    private Integer completionPercentage = 0;
    
    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

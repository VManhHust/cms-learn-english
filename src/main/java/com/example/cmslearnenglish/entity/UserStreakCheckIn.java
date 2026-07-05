package com.example.cmslearnenglish.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "user_streak_checkins",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_streak_checkin_date",
                columnNames = {"user_id", "check_in_date"}
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStreakCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

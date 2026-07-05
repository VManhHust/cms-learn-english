package com.example.cmslearnenglish.repository;

import com.example.cmslearnenglish.entity.UserStreakCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserStreakCheckInRepository extends JpaRepository<UserStreakCheckIn, Long> {

    @Query("""
            SELECT c.checkInDate
            FROM UserStreakCheckIn c
            WHERE c.user.id = :userId
            ORDER BY c.checkInDate DESC
            """)
    List<LocalDate> findCheckInDatesByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndCheckInDate(Long userId, LocalDate checkInDate);

    long countByUserId(Long userId);

    @Modifying
    @Query(
            value = """
                    INSERT INTO user_streak_checkins (user_id, check_in_date)
                    VALUES (:userId, :checkInDate)
                    ON CONFLICT (user_id, check_in_date) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("userId") Long userId,
            @Param("checkInDate") LocalDate checkInDate
    );
}

package com.example.cmslearnenglish.repository;

import com.example.cmslearnenglish.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /** Xóa tất cả token đã expired hoặc đã revoked của một user */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId AND (rt.revoked = true OR rt.expiresAt < :now)")
    void deleteExpiredOrRevokedByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /** Đếm số token còn active (chưa revoked, chưa expired) của một user */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.revoked = false AND rt.expiresAt >= :now")
    long countActiveByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /** Lấy danh sách token active cũ nhất của user (để revoke khi vượt giới hạn) */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.revoked = false AND rt.expiresAt >= :now ORDER BY rt.createdAt ASC")
    List<RefreshToken> findActiveByUserIdOrderByCreatedAtAsc(@Param("userId") Long userId, @Param("now") Instant now);
}

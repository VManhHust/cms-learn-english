package com.example.cmslearnenglish.repository;

import com.example.cmslearnenglish.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    /** Tìm record chưa dùng, chưa expired của email */
    @Query("SELECT ev FROM EmailVerification ev WHERE ev.email = :email AND ev.used = false AND ev.expiresAt > :now ORDER BY ev.createdAt DESC LIMIT 1")
    Optional<EmailVerification> findLatestValid(@Param("email") String email, @Param("now") Instant now);

    /** Xóa toàn bộ OTP cũ (đã dùng hoặc expired) của email để giữ DB gọn */
    @Modifying
    @Query("DELETE FROM EmailVerification ev WHERE ev.email = :email AND (ev.used = true OR ev.expiresAt <= :now)")
    void deleteExpiredOrUsedByEmail(@Param("email") String email, @Param("now") Instant now);

    /** Đếm số OTP chưa dùng còn hiệu lực — dùng cho rate limiting */
    @Query("SELECT COUNT(ev) FROM EmailVerification ev WHERE ev.email = :email AND ev.used = false AND ev.expiresAt > :now")
    long countActiveByEmail(@Param("email") String email, @Param("now") Instant now);
}

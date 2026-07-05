package com.example.cmslearnenglish.service;

public interface EmailVerificationService {

    /**
     * Tạo OTP 6 số, lưu hash vào DB, gửi email.
     * Ném IllegalStateException nếu đã có OTP active (rate-limit: 1 lần/phút).
     */
    void sendOtp(String email);

    /**
     * Kiểm tra OTP có hợp lệ không rồi đánh dấu đã dùng.
     * Ném IllegalArgumentException nếu OTP sai hoặc hết hạn.
     */
    void verifyOtp(String email, String code);

    /**
     * Chỉ kiểm tra OTP có hợp lệ, KHÔNG đánh dấu đã dùng.
     * Dùng cho forgot-password bước 2 (verify trước, consume khi reset).
     * Ném IllegalArgumentException nếu OTP sai hoặc hết hạn.
     */
    void checkOtp(String email, String code);
}

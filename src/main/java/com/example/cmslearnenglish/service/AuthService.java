package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.LoginRequest;
import com.example.cmslearnenglish.dto.LoginResponse;
import com.example.cmslearnenglish.dto.RegisterRequest;
import com.example.cmslearnenglish.dto.TokenPair;
import com.example.cmslearnenglish.entity.User;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse register(RegisterRequest request);
    TokenPair refresh(String rawRefreshToken);
    void logout(String rawRefreshToken);
    TokenPair generateTokenPair(User user);

    /**
     * Đặt lại mật khẩu mới (sau khi OTP đã được verify).
     * @param email    email tài khoản
     * @param otpCode  mã OTP 6 số (sẽ verify lại ở đây)
     * @param newPassword mật khẩu mới
     */
    void resetPassword(String email, String otpCode, String newPassword);
}

package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.entity.EmailVerification;
import com.example.cmslearnenglish.repository.EmailVerificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final EmailVerificationRepository repository;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailVerificationServiceImpl(EmailVerificationRepository repository,
                                        JavaMailSender mailSender) {
        this.repository = repository;
        this.mailSender = mailSender;
    }

    @Override
    @Transactional
    public void sendOtp(String email) {
        sendOtpWithPurpose(email, "Mã xác thực đăng ký tài khoản LinguaFlow của bạn là:");
    }

    @Transactional
    public void sendForgotPasswordOtp(String email) {
        sendOtpWithPurpose(email, "Mã xác thực đặt lại mật khẩu LinguaFlow của bạn là:");
    }

    private void sendOtpWithPurpose(String email, String purposeText) {
        Instant now = Instant.now();

        // Cleanup OTP cũ
        repository.deleteExpiredOrUsedByEmail(email, now);

        // Tạo OTP 6 số
        String rawCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        String codeHash = hashCode(rawCode);

        // Upsert: nếu đã có bản ghi active thì update, không tạo mới
        Optional<EmailVerification> existing = repository.findLatestValid(email, now);
        if (existing.isPresent()) {
            EmailVerification record = existing.get();
            // Rate limit: chặn gửi lại trong vòng RESEND_COOLDOWN_SECONDS
            long secondsSinceCreated = ChronoUnit.SECONDS.between(record.getCreatedAt(), now);
            if (secondsSinceCreated < RESEND_COOLDOWN_SECONDS) {
                throw new IllegalStateException("Vui lòng chờ " +
                        (RESEND_COOLDOWN_SECONDS - secondsSinceCreated) + " giây trước khi gửi lại.");
            }
            // Cập nhật code và thời hạn vào bản ghi cũ
            record.setCodeHash(codeHash);
            record.setExpiresAt(now.plus(otpExpiryMinutes, ChronoUnit.MINUTES));
            record.setCreatedAt(now);
            repository.save(record);
        } else {
            // Chưa có bản ghi active → tạo mới
            EmailVerification verification = EmailVerification.builder()
                    .email(email)
                    .codeHash(codeHash)
                    .expiresAt(now.plus(otpExpiryMinutes, ChronoUnit.MINUTES))
                    .build();
            repository.save(verification);
        }

        // Gửi email
        sendEmail(email, rawCode, purposeText);
    }

    @Override
    @Transactional
    public void verifyOtp(String email, String code) {
        Instant now = Instant.now();
        String codeHash = hashCode(code);

        EmailVerification verification = repository.findLatestValid(email, now)
                .orElseThrow(() -> new IllegalArgumentException("Mã xác thực không hợp lệ hoặc đã hết hạn."));

        if (!verification.getCodeHash().equals(codeHash)) {
            throw new IllegalArgumentException("Mã xác thực không đúng.");
        }

        // Đánh dấu đã dùng
        verification.setUsed(true);
        repository.save(verification);
    }

    @Override
    @Transactional(readOnly = true)
    public void checkOtp(String email, String code) {
        Instant now = Instant.now();
        String codeHash = hashCode(code);

        EmailVerification verification = repository.findLatestValid(email, now)
                .orElseThrow(() -> new IllegalArgumentException("Mã xác thực không hợp lệ hoặc đã hết hạn."));

        if (!verification.getCodeHash().equals(codeHash)) {
            throw new IllegalArgumentException("Mã xác thực không đúng.");
        }
        // Không đánh dấu used — sẽ consume khi reset password
    }

    private void sendEmail(String toEmail, String code, String purposeText) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Mã xác thực LinguaFlow");
            message.setText(
                    "Xin chào!\n\n" +
                    purposeText + "\n\n" +
                    "    " + code + "\n\n" +
                    "Mã có hiệu lực trong " + otpExpiryMinutes + " phút.\n" +
                    "Nếu bạn không yêu cầu, vui lòng bỏ qua email này.\n\n" +
                    "LinguaFlow Team"
            );
            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Không thể gửi email xác thực. Vui lòng thử lại sau.", e);
        }
    }

    private String hashCode(String rawCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawCode.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

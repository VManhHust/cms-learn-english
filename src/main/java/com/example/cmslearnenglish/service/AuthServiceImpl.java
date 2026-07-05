package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.LoginRequest;
import com.example.cmslearnenglish.dto.LoginResponse;
import com.example.cmslearnenglish.dto.RegisterRequest;
import com.example.cmslearnenglish.dto.TokenPair;
import com.example.cmslearnenglish.dto.UserDto;
import com.example.cmslearnenglish.entity.RefreshToken;
import com.example.cmslearnenglish.entity.User;
import com.example.cmslearnenglish.repository.RefreshTokenRepository;
import com.example.cmslearnenglish.repository.UserRepository;
import com.example.cmslearnenglish.security.JwtProvider;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           JwtProvider jwtProvider,
                           PasswordEncoder passwordEncoder,
                           EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getEmail())
                .build();
        user = userRepository.save(user);
        TokenPair tokenPair = generateTokenPair(user);
        UserDto userDto = toUserDto(user);
        return new LoginResponse(tokenPair.getAccessToken(), tokenPair.getRefreshToken(), userDto);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng");
        }

        TokenPair tokenPair = generateTokenPair(user);

        UserDto userDto = toUserDto(user);
        return new LoginResponse(tokenPair.getAccessToken(), tokenPair.getRefreshToken(), userDto);
    }

    @Override
    @Transactional
    public TokenPair generateTokenPair(User user) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name(), user.getDisplayName());
        String rawRefreshToken = jwtProvider.generateRefreshToken(user.getId());
        storeRefreshToken(user, rawRefreshToken);
        return new TokenPair(accessToken, rawRefreshToken);
    }

    @Override
    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new JwtException("Token invalid or expired"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new JwtException("Token invalid or expired");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        return generateTokenPair(user);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private static final int MAX_ACTIVE_SESSIONS = 5;

    private void storeRefreshToken(User user, String rawRefreshToken) {
        Instant now = Instant.now();

        // Xóa token đã expired hoặc revoked để giữ DB gọn
        refreshTokenRepository.deleteExpiredOrRevokedByUserId(user.getId(), now);

        // Nếu vẫn vượt giới hạn session, revoke session cũ nhất
        long activeCount = refreshTokenRepository.countActiveByUserId(user.getId(), now);
        if (activeCount >= MAX_ACTIVE_SESSIONS) {
            List<RefreshToken> oldest = refreshTokenRepository
                    .findActiveByUserIdOrderByCreatedAtAsc(user.getId(), now);
            long toRevoke = activeCount - MAX_ACTIVE_SESSIONS + 1;
            oldest.stream()
                    .limit(toRevoke)
                    .forEach(t -> {
                        t.setRevoked(true);
                        refreshTokenRepository.save(t);
                    });
        }

        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(now.plus(7, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private UserDto toUserDto(User user) {
        Instant now = Instant.now();
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                isProActive(user, now),
                user.getProStartsAt(),
                user.getProExpiresAt()
        );
    }

    private boolean isProActive(User user, Instant now) {
        boolean started = user.getProStartsAt() == null || !user.getProStartsAt().isAfter(now);
        return started && user.getProExpiresAt() != null && user.getProExpiresAt().isAfter(now);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword) {
        // Verify OTP (sẽ ném IllegalArgumentException nếu sai/hết hạn)
        emailVerificationService.verifyOtp(email, otpCode);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email này."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Thu hồi tất cả refresh token hiện có để buộc đăng nhập lại
        refreshTokenRepository.deleteExpiredOrRevokedByUserId(user.getId(), Instant.now());
    }
}

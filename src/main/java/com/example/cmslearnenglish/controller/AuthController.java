package com.example.cmslearnenglish.controller;

import com.example.cmslearnenglish.dto.ErrorResponse;
import com.example.cmslearnenglish.dto.LoginRequest;
import com.example.cmslearnenglish.dto.LoginResponse;
import com.example.cmslearnenglish.dto.TokenPair;
import com.example.cmslearnenglish.service.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "linguaflow_refresh_token";

    private final AuthService authService;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookies;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse servletResponse) {
        LoginResponse response = authService.login(request);
        setRefreshCookie(servletResponse, response.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse servletResponse) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Token invalid or expired"));
        }
        try {
            TokenPair tokenPair = authService.refresh(refreshToken);
            setRefreshCookie(servletResponse, tokenPair.getRefreshToken());
            return ResponseEntity.ok(tokenPair);
        } catch (JwtException e) {
            return ResponseEntity.status(401).body(new ErrorResponse("Token invalid or expired"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        clearRefreshCookie(response);
        return ResponseEntity.ok().build();
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, token);
        cookie.setMaxAge(60 * 60 * 24 * 7);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookies);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookies);
        response.addCookie(cookie);
    }
}

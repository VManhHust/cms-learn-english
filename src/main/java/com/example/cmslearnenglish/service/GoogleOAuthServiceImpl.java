package com.example.cmslearnenglish.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    // State token TTL: 10 phút
    private static final long STATE_TTL_SECONDS = 600;

    // Lưu state token tạm thời trong memory (key=state, value=expiry)
    // Trong môi trường multi-instance nên dùng Redis thay thế
    private final Map<String, Instant> pendingStates = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GoogleOAuthServiceImpl(ObjectMapper objectMapper) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
    }

    @Override
    public String[] buildAuthorizationUrl() {
        // Generate cryptographically secure state token
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // Lưu state với thời gian hết hạn
        pendingStates.put(state, Instant.now().plusSeconds(STATE_TTL_SECONDS));

        // Cleanup các state đã expired
        pendingStates.entrySet().removeIf(e -> e.getValue().isBefore(Instant.now()));

        String url = GOOGLE_AUTH_URL
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=email+profile"
                + "&state=" + state;

        return new String[]{url, state};
    }

    @Override
    public boolean isValidState(String state) {
        if (state == null) return false;
        Instant expiry = pendingStates.remove(state); // Dùng 1 lần duy nhất
        return expiry != null && expiry.isAfter(Instant.now());
    }

    @Override
    public GoogleUserInfo exchangeCodeForUserInfo(String authorizationCode) {
        try {
            String tokenRequestBody = "code=" + authorizationCode
                    + "&client_id=" + clientId
                    + "&client_secret=" + clientSecret
                    + "&redirect_uri=" + redirectUri
                    + "&grant_type=authorization_code";

            String tokenResponseBody = restClient.post()
                    .uri(GOOGLE_TOKEN_URL)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(tokenRequestBody)
                    .retrieve()
                    .body(String.class);

            TokenResponse tokenResponse = objectMapper.readValue(tokenResponseBody, TokenResponse.class);

            String userInfoBody = restClient.get()
                    .uri(GOOGLE_USERINFO_URL)
                    .header("Authorization", "Bearer " + tokenResponse.getAccessToken())
                    .retrieve()
                    .body(String.class);

            UserInfoResponse userInfo = objectMapper.readValue(userInfoBody, UserInfoResponse.class);

            return new GoogleUserInfo(userInfo.getEmail(), userInfo.getName(), userInfo.getId());
        } catch (Exception e) {
            throw new RuntimeException("oauth_failed", e);
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("token_type")
        private String tokenType;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class UserInfoResponse {
        @JsonProperty("id")
        private String id;
        @JsonProperty("email")
        private String email;
        @JsonProperty("name")
        private String name;
    }
}

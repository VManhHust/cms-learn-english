package com.example.cmslearnenglish.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public interface GoogleOAuthService {
    /**
     * Tạo authorization URL kèm state token để phòng CSRF.
     * @return mảng gồm [authorizationUrl, stateToken]
     */
    String[] buildAuthorizationUrl();
    GoogleUserInfo exchangeCodeForUserInfo(String authorizationCode);
    boolean isValidState(String state);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class GoogleUserInfo {
        private String email;
        private String name;
        private String googleId;
    }
}

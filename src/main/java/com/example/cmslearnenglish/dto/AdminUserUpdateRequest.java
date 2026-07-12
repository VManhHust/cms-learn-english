package com.example.cmslearnenglish.dto;

import com.example.cmslearnenglish.entity.enums.Role;
import com.example.cmslearnenglish.entity.enums.UserStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class AdminUserUpdateRequest {
    private String displayName;
    private Role role;
    private UserStatus status;
    private Instant proStartsAt;
    private Instant proExpiresAt;
}

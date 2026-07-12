package com.example.cmslearnenglish.dto;

import com.example.cmslearnenglish.entity.enums.Role;
import com.example.cmslearnenglish.entity.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class AdminUserCreateRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String displayName;
    private Role role = Role.USER;
    private UserStatus status = UserStatus.ACTIVE;
    private Instant proStartsAt;
    private Instant proExpiresAt;
}

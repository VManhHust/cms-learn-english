package com.example.cmslearnenglish.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ProStatusResponse {
    private boolean pro;
    private String currentPlanCode;
    private String currentPlanName;
    private Instant proStartsAt;
    private Instant proExpiresAt;
}

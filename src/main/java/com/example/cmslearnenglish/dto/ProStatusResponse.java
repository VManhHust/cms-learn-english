package com.example.cmslearnenglish.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import com.example.cmslearnenglish.entity.enums.ProPlan;

@Data
@Builder
public class ProStatusResponse {
    private boolean pro;
    private ProPlan currentPlanCode;
    private String currentPlanName;
    private Instant proStartsAt;
    private Instant proExpiresAt;
}

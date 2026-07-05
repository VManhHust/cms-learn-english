package com.example.cmslearnenglish.dto;

import com.example.cmslearnenglish.entity.enums.ProPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateProOrderRequest {

    @NotNull(message = "Plan is required")
    private ProPlan planCode;
}

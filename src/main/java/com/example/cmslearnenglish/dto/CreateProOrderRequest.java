package com.example.cmslearnenglish.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProOrderRequest {

    @NotBlank(message = "Plan is required")
    private String planCode;
}

package com.example.cmslearnenglish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitErrorResponse {
    private String error;
    private int retryAfter;
}

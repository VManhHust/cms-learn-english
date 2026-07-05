package com.example.cmslearnenglish.dto;

import com.example.cmslearnenglish.entity.enums.PaymentOrderStatus;
import com.example.cmslearnenglish.entity.enums.ProPlan;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PaymentOrderResponse {
    private UUID orderId;
    private ProPlan planCode;
    private String planName;
    private Long amount;
    private String currency;
    private PaymentOrderStatus status;
    private String paymentCode;
    private String qrCodeUrl;
    private String bank;
    private String accountNumber;
    private String accountHolder;
    private Instant expiresAt;
    private Instant paidAt;
    private Instant proStartsAt;
    private Instant proExpiresAt;
}

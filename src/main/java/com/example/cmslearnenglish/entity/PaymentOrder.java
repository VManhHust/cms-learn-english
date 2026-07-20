package com.example.cmslearnenglish.entity;

import com.example.cmslearnenglish.entity.enums.PaymentOrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "payment_code", nullable = false, unique = true, length = 20)
    private String paymentCode;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 20)
    private String planCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentOrderStatus status;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Instant paidAt;

    @Column
    private Instant proStartsAt;

    @Column
    private Instant proExpiresAt;

    @Column(unique = true)
    private Long sepayTransactionId;

    @Column(length = 255)
    private String bankReferenceCode;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}

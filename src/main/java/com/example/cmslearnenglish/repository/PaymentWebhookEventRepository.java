package com.example.cmslearnenglish.repository;

import com.example.cmslearnenglish.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO payment_webhook_events (transaction_id, raw_payload, received_at)
            VALUES (:transactionId, :rawPayload, :receivedAt)
            ON CONFLICT (transaction_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("transactionId") Long transactionId,
            @Param("rawPayload") String rawPayload,
            @Param("receivedAt") Instant receivedAt
    );
}

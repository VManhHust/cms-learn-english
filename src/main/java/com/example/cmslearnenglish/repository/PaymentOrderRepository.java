package com.example.cmslearnenglish.repository;

import com.example.cmslearnenglish.entity.PaymentOrder;
import com.example.cmslearnenglish.entity.enums.PaymentOrderStatus;
import com.example.cmslearnenglish.entity.enums.ProPlan;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {

    Optional<PaymentOrder> findByIdAndUserId(UUID id, Long userId);

    Optional<PaymentOrder> findFirstByUserIdAndPlanCodeAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId,
            ProPlan planCode,
            PaymentOrderStatus status,
            Instant now
    );

    Optional<PaymentOrder> findFirstByUserIdAndStatusOrderByPaidAtDescCreatedAtDesc(
            Long userId,
            PaymentOrderStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select paymentOrder from PaymentOrder paymentOrder where paymentOrder.paymentCode = :paymentCode")
    Optional<PaymentOrder> findByPaymentCodeForUpdate(@Param("paymentCode") String paymentCode);
}

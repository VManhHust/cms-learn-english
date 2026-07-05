package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.PaymentOrderResponse;
import com.example.cmslearnenglish.dto.ProStatusResponse;
import com.example.cmslearnenglish.dto.SepayWebhookPayload;
import com.example.cmslearnenglish.entity.PaymentOrder;
import com.example.cmslearnenglish.entity.enums.PaymentOrderStatus;
import com.example.cmslearnenglish.entity.enums.ProPlan;
import com.example.cmslearnenglish.entity.User;
import com.example.cmslearnenglish.exception.ResourceNotFoundException;
import com.example.cmslearnenglish.repository.PaymentOrderRepository;
import com.example.cmslearnenglish.repository.PaymentWebhookEventRepository;
import com.example.cmslearnenglish.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentService {

    private static final Pattern PAYMENT_CODE_PATTERN = Pattern.compile("\\bPRO[A-Z0-9]{10}\\b");

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${payment.order.expiry-minutes:30}")
    private long orderExpiryMinutes;

    @Value("${payment.sepay.bank:}")
    private String bank;

    @Value("${payment.sepay.account-number:}")
    private String accountNumber;

    @Value("${payment.sepay.account-holder:}")
    private String accountHolder;

    public PaymentOrderResponse createProOrder(Long userId, ProPlan plan) {
        validatePaymentConfiguration();
        User user = findUser(userId);
        Instant now = Instant.now();
        validateUpgradePlan(userId, user, plan, now);

        Optional<PaymentOrder> currentOrder =
                paymentOrderRepository.findFirstByUserIdAndPlanCodeAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId,
                        plan,
                        PaymentOrderStatus.PENDING,
                        now
                );
        if (currentOrder.isPresent()) {
            return toResponse(currentOrder.get());
        }

        PaymentOrder order = PaymentOrder.builder()
                .id(UUID.randomUUID())
                .user(user)
                .paymentCode(generatePaymentCode())
                .amount(plan.getAmount())
                .planCode(plan)
                .status(PaymentOrderStatus.PENDING)
                .expiresAt(now.plus(orderExpiryMinutes, ChronoUnit.MINUTES))
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toResponse(paymentOrderRepository.save(order));
    }

    private void validateUpgradePlan(Long userId, User user, ProPlan requestedPlan, Instant now) {
        if (!isProActive(user, now)) {
            return;
        }

        paymentOrderRepository.findFirstByUserIdAndStatusOrderByPaidAtDescCreatedAtDesc(
                userId,
                PaymentOrderStatus.PAID
        ).ifPresent(currentOrder -> {
            ProPlan currentPlan = currentOrder.getPlanCode();
            if (requestedPlan.getRank() <= currentPlan.getRank()) {
                throw new ResponseStatusException(
                        BAD_REQUEST,
                        "Please choose a higher PRO plan to upgrade"
                );
            }
        });
    }

    public PaymentOrderResponse getOrder(Long userId, UUID orderId) {
        PaymentOrder order = paymentOrderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));

        if (order.getStatus() == PaymentOrderStatus.PENDING && order.getExpiresAt().isBefore(Instant.now())) {
            order.setStatus(PaymentOrderStatus.EXPIRED);
            order.setUpdatedAt(Instant.now());
        }

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public ProStatusResponse getProStatus(Long userId) {
        User user = findUser(userId);
        Instant now = Instant.now();
        Optional<PaymentOrder> currentOrder =
                paymentOrderRepository.findFirstByUserIdAndStatusOrderByPaidAtDescCreatedAtDesc(
                        userId,
                        PaymentOrderStatus.PAID
                );
        return ProStatusResponse.builder()
                .pro(isProActive(user, now))
                .currentPlanCode(currentOrder.map(PaymentOrder::getPlanCode).orElse(null))
                .currentPlanName(currentOrder.map(order -> order.getPlanCode().getDisplayName()).orElse(null))
                .proStartsAt(user.getProStartsAt())
                .proExpiresAt(user.getProExpiresAt())
                .build();
    }

    public void processSepayWebhook(String rawPayload) {
        SepayWebhookPayload payload = parsePayload(rawPayload);
        validateRequiredWebhookFields(payload);

        if (webhookEventRepository.insertIfAbsent(payload.getId(), rawPayload, Instant.now()) == 0) {
            return;
        }

        if (!"in".equalsIgnoreCase(payload.getTransferType())) {
            log.info("Ignoring outgoing SePay transaction {}", payload.getId());
            return;
        }

        String paymentCode = resolvePaymentCode(payload);
        if (paymentCode == null) {
            log.warn("No PRO payment code found in SePay transaction {}", payload.getId());
            return;
        }

        Optional<PaymentOrder> optionalOrder = paymentOrderRepository.findByPaymentCodeForUpdate(paymentCode);
        if (optionalOrder.isEmpty()) {
            log.warn("No payment order found for SePay transaction {}, code {}", payload.getId(), paymentCode);
            return;
        }

        PaymentOrder order = optionalOrder.get();
        if (order.getStatus() == PaymentOrderStatus.PAID) {
            return;
        }
        if (!normalizeAccount(accountNumber).equals(normalizeAccount(payload.getAccountNumber()))) {
            log.warn("Account mismatch for SePay transaction {}", payload.getId());
            return;
        }
        if (!order.getAmount().equals(payload.getTransferAmount())) {
            log.warn("Amount mismatch for SePay transaction {}", payload.getId());
            return;
        }

        Instant now = Instant.now();
        User user = userRepository.findByIdForUpdate(order.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ProPeriod period = activatePro(user, order.getPlanCode(), now);

        order.setStatus(PaymentOrderStatus.PAID);
        order.setPaidAt(now);
        order.setProStartsAt(period.startsAt());
        order.setProExpiresAt(period.expiresAt());
        order.setSepayTransactionId(payload.getId());
        order.setBankReferenceCode(payload.getReferenceCode());
        order.setUpdatedAt(now);
    }

    private PaymentOrderResponse toResponse(PaymentOrder order) {
        return PaymentOrderResponse.builder()
                .orderId(order.getId())
                .planCode(order.getPlanCode())
                .planName(order.getPlanCode().getDisplayName())
                .amount(order.getAmount())
                .currency("VND")
                .status(order.getStatus())
                .paymentCode(order.getPaymentCode())
                .qrCodeUrl(buildQrCodeUrl(order))
                .bank(bank)
                .accountNumber(accountNumber)
                .accountHolder(accountHolder)
                .expiresAt(order.getExpiresAt())
                .paidAt(order.getPaidAt())
                .proStartsAt(order.getProStartsAt() != null
                        ? order.getProStartsAt()
                        : order.getUser().getProStartsAt())
                .proExpiresAt(order.getProExpiresAt() != null
                        ? order.getProExpiresAt()
                        : order.getUser().getProExpiresAt())
                .build();
    }

    private ProPeriod activatePro(User user, ProPlan plan, Instant now) {
        Instant currentExpiry = user.getProExpiresAt();
        boolean currentlyActive = isProActive(user, now);
        Instant grantedStartsAt = currentlyActive && !ProPlan.isLifetimeExpiry(currentExpiry)
                ? currentExpiry
                : now;
        Instant newExpiry = plan.calculateExpiry(currentExpiry, now);

        if (!currentlyActive || user.getProStartsAt() == null) {
            user.setProStartsAt(now);
        }
        user.setProExpiresAt(newExpiry);

        return new ProPeriod(grantedStartsAt, newExpiry);
    }

    private boolean isProActive(User user, Instant now) {
        boolean started = user.getProStartsAt() == null || !user.getProStartsAt().isAfter(now);
        return started && user.getProExpiresAt() != null && user.getProExpiresAt().isAfter(now);
    }

    private static class ProPeriod {
        private final Instant startsAt;
        private final Instant expiresAt;

        private ProPeriod(Instant startsAt, Instant expiresAt) {
            this.startsAt = startsAt;
            this.expiresAt = expiresAt;
        }

        private Instant startsAt() {
            return startsAt;
        }

        private Instant expiresAt() {
            return expiresAt;
        }
    }

    private String buildQrCodeUrl(PaymentOrder order) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://qr.sepay.vn/img")
                .queryParam("acc", accountNumber)
                .queryParam("bank", bank)
                .queryParam("amount", order.getAmount())
                .queryParam("des", order.getPaymentCode())
                .queryParam("template", "compact")
                .queryParam("showinfo", true);
        if (accountHolder != null && !accountHolder.isBlank()) {
            builder.queryParam("holder", accountHolder);
        }
        return builder.build().encode().toUriString();
    }

    private SepayWebhookPayload parsePayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, SepayWebhookPayload.class);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid SePay payload");
        }
    }

    private void validateRequiredWebhookFields(SepayWebhookPayload payload) {
        if (payload.getId() == null || payload.getTransferAmount() == null || payload.getAccountNumber() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Missing required SePay fields");
        }
    }

    private String resolvePaymentCode(SepayWebhookPayload payload) {
        if (payload.getCode() != null) {
            String normalizedCode = payload.getCode().trim().toUpperCase(Locale.ROOT);
            if (PAYMENT_CODE_PATTERN.matcher(normalizedCode).matches()) {
                return normalizedCode;
            }
        }
        if (payload.getContent() == null) {
            return null;
        }
        Matcher matcher = PAYMENT_CODE_PATTERN.matcher(payload.getContent().toUpperCase(Locale.ROOT));
        return matcher.find() ? matcher.group() : null;
    }

    private String generatePaymentCode() {
        return "PRO" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 10)
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeAccount(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private void validatePaymentConfiguration() {
        if (bank == null || bank.isBlank() || accountNumber == null || accountNumber.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "SePay bank account is not configured");
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }
}

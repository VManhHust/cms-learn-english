package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminProService {

    private static final int MAX_PAGE_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public Page<ProPlanDto> getPlans(int page, int size, String sort, String order, String q, String status) {
        PageRequest pageable = pageRequest(page, size);
        List<Object> args = new ArrayList<>();
        String where = planWhere(q, status, args);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pro_plan_configs p " + where, Long.class, args.toArray());

        args.add(pageable.getPageSize());
        args.add(pageable.getOffset());
        List<ProPlanDto> content = jdbcTemplate.query("""
                SELECT p.*,
                       (SELECT COUNT(*) FROM payment_orders o WHERE o.plan_code = p.code)::int order_count
                FROM pro_plan_configs p
                """ + where + " ORDER BY " + planSort(sort) + direction(order) + " LIMIT ? OFFSET ?",
                this::mapPlan,
                args.toArray());
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public ProPlanDto getPlan(Long id) {
        List<ProPlanDto> rows = jdbcTemplate.query("""
                SELECT p.*,
                       (SELECT COUNT(*) FROM payment_orders o WHERE o.plan_code = p.code)::int order_count
                FROM pro_plan_configs p
                WHERE p.id = ?
                """, this::mapPlan, id);
        return rows.stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("PRO plan not found"));
    }

    @Transactional
    public ProPlanDto createPlan(ProPlanRequest request) {
        try {
            Long id = jdbcTemplate.queryForObject("""
                    INSERT INTO pro_plan_configs
                        (code, name, description, amount, duration_days, benefits, special_benefits,
                         status, featured, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """, Long.class,
                    normalizeCode(request.code()),
                    trimRequired(request.name(), "Plan name is required"),
                    trim(request.description()),
                    normalizeAmount(request.amount()),
                    request.durationDays(),
                    trim(request.benefits()),
                    trim(request.specialBenefits()),
                    normalizeStatus(request.status()),
                    Boolean.TRUE.equals(request.featured()),
                    defaultInt(request.sortOrder()));
            return getPlan(id);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("Plan code already exists: " + request.code());
        }
    }

    @Transactional
    public ProPlanDto updatePlan(Long id, ProPlanRequest request) {
        requirePlan(id);
        try {
            jdbcTemplate.update("""
                    UPDATE pro_plan_configs
                    SET code = ?, name = ?, description = ?, amount = ?, duration_days = ?,
                        benefits = ?, special_benefits = ?, status = ?, featured = ?,
                        sort_order = ?, updated_at = NOW()
                    WHERE id = ?
                    """,
                    normalizeCode(request.code()),
                    trimRequired(request.name(), "Plan name is required"),
                    trim(request.description()),
                    normalizeAmount(request.amount()),
                    request.durationDays(),
                    trim(request.benefits()),
                    trim(request.specialBenefits()),
                    normalizeStatus(request.status()),
                    Boolean.TRUE.equals(request.featured()),
                    defaultInt(request.sortOrder()),
                    id);
            return getPlan(id);
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("Plan code already exists: " + request.code());
        }
    }

    @Transactional
    public void deletePlan(Long id) {
        requirePlan(id);
        jdbcTemplate.update("DELETE FROM pro_plan_configs WHERE id = ?", id);
    }

    @Transactional(readOnly = true)
    public Page<PaymentOrderDto> getOrders(
            int page,
            int size,
            String sort,
            String order,
            String q,
            String status,
            String planCode) {
        PageRequest pageable = pageRequest(page, size);
        List<Object> args = new ArrayList<>();
        String where = orderWhere(q, status, planCode, args);
        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM payment_orders o
                JOIN users u ON u.id = o.user_id
                LEFT JOIN pro_plan_configs p ON p.code = o.plan_code
                """ + where, Long.class, args.toArray());

        args.add(pageable.getPageSize());
        args.add(pageable.getOffset());
        List<PaymentOrderDto> content = jdbcTemplate.query("""
                SELECT o.id, o.user_id, u.email user_email, u.display_name user_name,
                       o.payment_code, o.amount, o.plan_code, COALESCE(p.name, o.plan_code) plan_name,
                       o.status, o.expires_at, o.paid_at, o.pro_starts_at, o.pro_expires_at,
                       o.sepay_transaction_id, o.bank_reference_code, o.created_at, o.updated_at
                FROM payment_orders o
                JOIN users u ON u.id = o.user_id
                LEFT JOIN pro_plan_configs p ON p.code = o.plan_code
                """ + where + " ORDER BY " + orderSort(sort) + direction(order) + " LIMIT ? OFFSET ?",
                this::mapOrder,
                args.toArray());
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public PaymentOrderDto getOrder(UUID id) {
        List<PaymentOrderDto> rows = jdbcTemplate.query("""
                SELECT o.id, o.user_id, u.email user_email, u.display_name user_name,
                       o.payment_code, o.amount, o.plan_code, COALESCE(p.name, o.plan_code) plan_name,
                       o.status, o.expires_at, o.paid_at, o.pro_starts_at, o.pro_expires_at,
                       o.sepay_transaction_id, o.bank_reference_code, o.created_at, o.updated_at
                FROM payment_orders o
                JOIN users u ON u.id = o.user_id
                LEFT JOIN pro_plan_configs p ON p.code = o.plan_code
                WHERE o.id = ?
                """, this::mapOrder, id);
        return rows.stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
    }

    private ProPlanDto mapPlan(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new ProPlanDto(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getLong("amount"),
                rs.getObject("duration_days", Integer.class),
                rs.getString("benefits"),
                rs.getString("special_benefits"),
                rs.getString("status"),
                rs.getBoolean("featured"),
                rs.getInt("sort_order"),
                rs.getInt("order_count"),
                instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("updated_at"))
        );
    }

    private PaymentOrderDto mapOrder(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        UUID id = (UUID) rs.getObject("id");
        return new PaymentOrderDto(
                id.toString(),
                rs.getLong("user_id"),
                rs.getString("user_email"),
                rs.getString("user_name"),
                rs.getString("payment_code"),
                rs.getLong("amount"),
                rs.getString("plan_code"),
                rs.getString("plan_name"),
                rs.getString("status"),
                instant(rs.getTimestamp("expires_at")),
                instant(rs.getTimestamp("paid_at")),
                instant(rs.getTimestamp("pro_starts_at")),
                instant(rs.getTimestamp("pro_expires_at")),
                rs.getObject("sepay_transaction_id", Long.class),
                rs.getString("bank_reference_code"),
                instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("updated_at"))
        );
    }

    private String planWhere(String q, String status, List<Object> args) {
        List<String> conditions = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            conditions.add("(LOWER(p.code) LIKE ? OR LOWER(p.name) LIKE ? OR LOWER(COALESCE(p.description, '')) LIKE ?)");
            String keyword = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            args.add(keyword);
            args.add(keyword);
            args.add(keyword);
        }
        if (status != null && !status.isBlank()) {
            conditions.add("p.status = ?");
            args.add(normalizeStatus(status));
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private String orderWhere(String q, String status, String planCode, List<Object> args) {
        List<String> conditions = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            conditions.add("""
                    (LOWER(o.payment_code) LIKE ? OR LOWER(o.plan_code) LIKE ?
                     OR LOWER(u.email) LIKE ? OR LOWER(COALESCE(u.display_name, '')) LIKE ?
                     OR LOWER(COALESCE(o.bank_reference_code, '')) LIKE ?)
                    """);
            String keyword = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            args.add(keyword);
            args.add(keyword);
            args.add(keyword);
            args.add(keyword);
            args.add(keyword);
        }
        if (status != null && !status.isBlank()) {
            conditions.add("o.status = ?");
            args.add(status.trim().toUpperCase(Locale.ROOT));
        }
        if (planCode != null && !planCode.isBlank()) {
            conditions.add("o.plan_code = ?");
            args.add(normalizeCode(planCode));
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private String planSort(String sort) {
        return switch (sort == null ? "" : sort) {
            case "code" -> "p.code";
            case "name" -> "p.name";
            case "amount" -> "p.amount";
            case "durationDays" -> "p.duration_days";
            case "status" -> "p.status";
            case "featured" -> "p.featured";
            case "createdAt" -> "p.created_at";
            case "updatedAt" -> "p.updated_at";
            case "orderCount" -> "order_count";
            default -> "p.sort_order";
        };
    }

    private String orderSort(String sort) {
        return switch (sort == null ? "" : sort) {
            case "paymentCode" -> "o.payment_code";
            case "userEmail" -> "u.email";
            case "amount" -> "o.amount";
            case "planCode" -> "o.plan_code";
            case "status" -> "o.status";
            case "expiresAt" -> "o.expires_at";
            case "paidAt" -> "o.paid_at";
            case "updatedAt" -> "o.updated_at";
            default -> "o.created_at";
        };
    }

    private String direction(String order) {
        return "ASC".equalsIgnoreCase(order) ? " ASC" : " DESC";
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, MAX_PAGE_SIZE)));
    }

    private String normalizeCode(String value) {
        String code = trimRequired(value, "Plan code is required").toUpperCase(Locale.ROOT);
        if (code.length() > 20) {
            throw new IllegalArgumentException("Plan code must be 20 characters or fewer");
        }
        return code;
    }

    private Long normalizeAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Plan amount must be greater than 0");
        }
        return amount;
    }

    private String normalizeStatus(String value) {
        String status = value == null || value.isBlank() ? "ACTIVE" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ACTIVE", "INACTIVE").contains(status)) {
            throw new IllegalArgumentException("Plan status must be ACTIVE or INACTIVE");
        }
        return status;
    }

    private String trimRequired(String value, String message) {
        String trimmed = trim(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }

    private String trim(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private void requirePlan(Long id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pro_plan_configs WHERE id = ?", Integer.class, id);
        if (count == null || count == 0) {
            throw new ResourceNotFoundException("PRO plan not found");
        }
    }

    public record ProPlanRequest(
            String code,
            String name,
            String description,
            Long amount,
            Integer durationDays,
            String benefits,
            String specialBenefits,
            String status,
            Boolean featured,
            Integer sortOrder
    ) {
    }

    public record ProPlanDto(
            Long id,
            String code,
            String name,
            String description,
            Long amount,
            Integer durationDays,
            String benefits,
            String specialBenefits,
            String status,
            Boolean featured,
            Integer sortOrder,
            Integer orderCount,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record PaymentOrderDto(
            String id,
            Long userId,
            String userEmail,
            String userName,
            String paymentCode,
            Long amount,
            String planCode,
            String planName,
            String status,
            Instant expiresAt,
            Instant paidAt,
            Instant proStartsAt,
            Instant proExpiresAt,
            Long sepayTransactionId,
            String bankReferenceCode,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}

package com.example.cmslearnenglish.controller;

import com.example.cmslearnenglish.service.AdminProService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/pro")
@RequiredArgsConstructor
public class AdminProController {

    private final AdminProService adminProService;

    @GetMapping("/plans")
    public Page<AdminProService.ProPlanDto> getPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "sortOrder") String sort,
            @RequestParam(defaultValue = "ASC") String order,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status) {
        return adminProService.getPlans(page, size, sort, order, q, status);
    }

    @GetMapping("/plans/{id}")
    public AdminProService.ProPlanDto getPlan(@PathVariable Long id) {
        return adminProService.getPlan(id);
    }

    @PostMapping("/plans")
    public AdminProService.ProPlanDto createPlan(@RequestBody AdminProService.ProPlanRequest request) {
        return adminProService.createPlan(request);
    }

    @PutMapping("/plans/{id}")
    public AdminProService.ProPlanDto updatePlan(
            @PathVariable Long id,
            @RequestBody AdminProService.ProPlanRequest request) {
        return adminProService.updatePlan(id, request);
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        adminProService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders")
    public Page<AdminProService.PaymentOrderDto> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String order,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String planCode) {
        return adminProService.getOrders(page, size, sort, order, q, status, planCode);
    }

    @GetMapping("/orders/{id}")
    public AdminProService.PaymentOrderDto getOrder(@PathVariable UUID id) {
        return adminProService.getOrder(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}

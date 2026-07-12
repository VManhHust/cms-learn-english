package com.example.cmslearnenglish.controller;

import com.example.cmslearnenglish.dto.AdminTopicRequest;
import com.example.cmslearnenglish.dto.AdminUserCreateRequest;
import com.example.cmslearnenglish.dto.AdminUserUpdateRequest;
import com.example.cmslearnenglish.dto.LearningExerciseDto;
import com.example.cmslearnenglish.dto.UserDto;
import com.example.cmslearnenglish.service.AdminCmsService;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCmsController {

    private final AdminCmsService adminCmsService;

    @GetMapping("/dashboard")
    public AdminCmsService.DashboardSummary getDashboard() {
        return adminCmsService.getDashboardSummary();
    }

    @GetMapping("/topics")
    public Page<AdminCmsService.AdminTopicDto> getTopics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "DESC") String order) {
        return adminCmsService.getTopics(page, size, sort, order);
    }

    @GetMapping("/topics/{id}")
    public AdminCmsService.AdminTopicDto getTopic(@PathVariable Long id) {
        return adminCmsService.getTopic(id);
    }

    @PostMapping("/topics")
    public AdminCmsService.AdminTopicDto createTopic(@Valid @RequestBody AdminTopicRequest request) {
        return adminCmsService.createTopic(request);
    }

    @PutMapping("/topics/{id}")
    public AdminCmsService.AdminTopicDto updateTopic(
            @PathVariable Long id,
            @Valid @RequestBody AdminTopicRequest request) {
        return adminCmsService.updateTopic(id, request);
    }

    @DeleteMapping("/topics/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable Long id) {
        adminCmsService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lessons")
    public Page<LearningExerciseDto> getLessons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String order,
            @RequestParam(required = false) String q) {
        return adminCmsService.getLessons(page, size, sort, order, q);
    }

    @GetMapping("/lessons/{id}")
    public LearningExerciseDto getLesson(@PathVariable Long id) {
        return adminCmsService.getLesson(id);
    }

    @GetMapping("/users")
    public Page<UserDto> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String order,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String pro) {
        return adminCmsService.getUsers(page, size, sort, order, q, displayName, role, status, pro);
    }

    @GetMapping("/users/{id}")
    public UserDto getUser(@PathVariable Long id) {
        return adminCmsService.getUser(id);
    }

    @PostMapping("/users")
    public UserDto createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        return adminCmsService.createUser(request);
    }

    @PutMapping("/users/{id}")
    public UserDto updateUser(
            @PathVariable Long id,
            @RequestBody AdminUserUpdateRequest request) {
        return adminCmsService.updateUser(id, request);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminCmsService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}

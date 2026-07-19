package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.AdminTopicRequest;
import com.example.cmslearnenglish.dto.AdminUserCreateRequest;
import com.example.cmslearnenglish.dto.AdminUserUpdateRequest;
import com.example.cmslearnenglish.dto.LearningExerciseDto;
import com.example.cmslearnenglish.dto.UserDto;
import com.example.cmslearnenglish.entity.LearningExercise;
import com.example.cmslearnenglish.entity.LearningTopic;
import com.example.cmslearnenglish.entity.User;
import com.example.cmslearnenglish.entity.enums.Role;
import com.example.cmslearnenglish.entity.enums.UserStatus;
import com.example.cmslearnenglish.repository.LearningExerciseRepository;
import com.example.cmslearnenglish.repository.LearningTopicRepository;
import com.example.cmslearnenglish.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCmsService {

    private final LearningTopicRepository topicRepository;
    private final LearningExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final YoutubeExerciseService youtubeExerciseService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary() {
        long activeProUsers = userRepository.countByProExpiresAtAfter(Instant.now());
        return new DashboardSummary(
                userRepository.count(),
                activeProUsers,
                topicRepository.count(),
                exerciseRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public Page<AdminTopicDto> getTopics(int page, int size, String sort, String order, String query) {
        Pageable pageable = PageRequest.of(page, size, toSort(sort, order, "id"));
        Page<LearningTopic> topics = query == null || query.isBlank()
                ? topicRepository.findAll(pageable)
                : topicRepository.findByTopicNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        query.trim(), query.trim(), pageable);
        return topics.map(this::toTopicDto);
    }

    @Transactional(readOnly = true)
    public AdminTopicDto getTopic(Long id) {
        return toTopicDto(requireTopic(id));
    }

    @Transactional
    public AdminTopicDto createTopic(AdminTopicRequest request) {
        LearningTopic topic = LearningTopic.builder()
                .topicName(request.getTopicName().trim())
                .description(request.getDescription())
                .type(request.getType())
                .build();
        return toTopicDto(topicRepository.save(topic));
    }

    @Transactional
    public AdminTopicDto updateTopic(Long id, AdminTopicRequest request) {
        LearningTopic topic = requireTopic(id);
        topic.setTopicName(request.getTopicName().trim());
        topic.setDescription(request.getDescription());
        topic.setType(request.getType());
        return toTopicDto(topicRepository.save(topic));
    }

    @Transactional
    public void deleteTopic(Long id) {
        if (exerciseRepository.existsByLearningTopicId(id)) {
            throw new IllegalArgumentException("Cannot delete a topic that still contains lessons");
        }
        topicRepository.delete(requireTopic(id));
    }

    @Transactional(readOnly = true)
    public Page<LearningExerciseDto> getLessons(int page, int size, String sort, String order, String query) {
        Pageable pageable = PageRequest.of(page, size, toSort(sort, order, "createdAt"));
        Page<LearningExercise> lessons = query == null || query.isBlank()
                ? exerciseRepository.findAll(pageable)
                : exerciseRepository.findByTitleContainingIgnoreCase(query.trim(), pageable);
        return lessons.map(exercise -> youtubeExerciseService.toExerciseDtoPublic(
                exercise, exercise.getYoutubeExerciseExtension()));
    }

    @Transactional(readOnly = true)
    public LearningExerciseDto getLesson(Long id) {
        LearningExercise exercise = exerciseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + id));
        return youtubeExerciseService.toExerciseDtoPublic(exercise, exercise.getYoutubeExerciseExtension());
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getUsers(
            int page,
            int size,
            String sort,
            String order,
            String query,
            String displayName,
            String role,
            String status,
            String pro) {
        Pageable pageable = PageRequest.of(page, size, toUserSort(sort, order));
        List<Role> roles = parseRoles(role);
        List<UserStatus> statuses = parseStatuses(status);
        boolean rolesEmpty = roles.isEmpty();
        boolean statusesEmpty = statuses.isEmpty();
        String normalizedEmail = normalize(query);
        String normalizedDisplayName = normalize(displayName);
        Page<User> users = userRepository.findByFilters(
                normalizedEmail == null ? "" : normalizedEmail,
                normalizedEmail == null,
                normalizedDisplayName == null ? "" : normalizedDisplayName,
                normalizedDisplayName == null,
                rolesEmpty ? List.of(Role.USER, Role.ADMIN) : roles,
                rolesEmpty,
                statusesEmpty ? List.of(UserStatus.ACTIVE, UserStatus.LOCK, UserStatus.DELETE) : statuses,
                statusesEmpty,
                parseProFilter(pro),
                Instant.now(),
                pageable
        );
        return users.map(this::toUserDto);
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        return toUserDto(requireUser(id));
    }

    @Transactional
    public UserDto createUser(AdminUserCreateRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(existing -> {
            throw new IllegalArgumentException("Email already exists: " + email);
        });

        String displayName = request.getDisplayName() == null || request.getDisplayName().isBlank()
                ? email
                : request.getDisplayName().trim();
        Role role = request.getRole() == null ? Role.USER : request.getRole();
        UserStatus status = request.getStatus() == null ? UserStatus.ACTIVE : request.getStatus();

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(displayName)
                .role(role)
                .status(status)
                .proStartsAt(request.getProStartsAt())
                .proExpiresAt(request.getProExpiresAt())
                .build();

        return toUserDto(userRepository.save(user));
    }

    @Transactional
    public UserDto updateUser(Long id, AdminUserUpdateRequest request) {
        User user = requireUser(id);
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        user.setProStartsAt(request.getProStartsAt());
        user.setProExpiresAt(request.getProExpiresAt());
        return toUserDto(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = requireUser(id);
        user.setStatus(UserStatus.DELETE);
        userRepository.save(user);
    }

    private LearningTopic requireTopic(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + id));
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    private AdminTopicDto toTopicDto(LearningTopic topic) {
        return new AdminTopicDto(
                topic.getId(),
                topic.getTopicName(),
                topic.getDescription(),
                topic.getType().name(),
                exerciseRepository.countByLearningTopicId(topic.getId()),
                topic.getCreatedAt()
        );
    }

    private UserDto toUserDto(User user) {
        Instant now = Instant.now();
        boolean started = user.getProStartsAt() == null || !user.getProStartsAt().isAfter(now);
        boolean pro = started && user.getProExpiresAt() != null && user.getProExpiresAt().isAfter(now);
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                pro,
                user.getProStartsAt(),
                user.getProExpiresAt(),
                user.getStatus().name()
        );
    }

    private Sort toSort(String field, String order, String fallback) {
        List<String> allowedFields = List.of("id", "createdAt", "title", "topicName", "email", "displayName", "status");
        String safeField = allowedFields.contains(field) ? field : fallback;
        Sort.Direction direction = "ASC".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, safeField);
    }

    private Sort toUserSort(String field, String order) {
        List<String> allowedFields = List.of("id", "createdAt", "email", "displayName", "role", "status", "statusOrder", "proStartsAt", "proExpiresAt");
        String safeField = allowedFields.contains(field) ? field : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort statusLast = Sort.by(Sort.Direction.ASC, "statusOrder");
        if ("statusOrder".equals(safeField)) {
            return statusLast.and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return statusLast.and(Sort.by(direction, safeField));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<Role> parseRoles(String value) {
        return splitValues(value).stream()
                .map(Role::valueOf)
                .toList();
    }

    private List<UserStatus> parseStatuses(String value) {
        return splitValues(value).stream()
                .map(UserStatus::valueOf)
                .toList();
    }

    private Boolean parseProFilter(String value) {
        List<String> values = splitValues(value);
        if (values.isEmpty() || values.size() > 1) {
            return null;
        }
        return Boolean.valueOf(values.get(0));
    }

    private List<String> splitValues(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(",")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(fluent = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class DashboardSummary {
        private long users;
        private long activeProUsers;
        private long topics;
        private long lessons;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(fluent = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public static class AdminTopicDto {
        private Long id;
        private String topicName;
        private String description;
        private String type;
        private long lessonCount;
        private Instant createdAt;
    }
}

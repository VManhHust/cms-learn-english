package com.example.cmslearnenglish.controller;

import com.example.cmslearnenglish.dto.*;
import com.example.cmslearnenglish.service.AdminLessonService;
import com.example.cmslearnenglish.service.AdminTranscriptService;
import com.example.cmslearnenglish.service.AdminTranscriptImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/lessons")
@RequiredArgsConstructor
public class AdminLessonController {

    private final AdminLessonService adminLessonService;
    private final AdminTranscriptService adminTranscriptService;
    private final AdminTranscriptImportService adminTranscriptImportService;

    /**
     * Import single lesson from YouTube
     * POST /api/admin/lessons/import
     * Body: { "topicId", "youtubeUrl", "title", "level", "channelYoutubeId" }
     */
    @PostMapping("/import")
    public ResponseEntity<LearningExerciseDto> importLesson(@RequestBody ImportLessonRequest request) {
        LearningExerciseDto lesson = adminLessonService.importLesson(
                request.getTopicId(), request.getYoutubeUrl(), request.getTitle(),
                request.getLevel(), request.getChannelYoutubeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(lesson);
    }

    @PostMapping("/import-transcript")
    public ResponseEntity<LearningExerciseDto> importTranscript(@RequestBody @jakarta.validation.Valid ImportTranscriptRequest request) {
        LearningExerciseDto lesson = adminTranscriptImportService.importByVideoId(request.getVideoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(lesson);
    }

    /**
     * Batch import lessons
     * POST /api/admin/lessons/batch-import
     * Body: { "topicId", "channelYoutubeId", "lessons": [{ "videoId", "title", "vocabularyLevel" }] }
     */
    @PostMapping("/batch-import")
    public ResponseEntity<List<LearningExerciseDto>> batchImport(@RequestBody BatchImportRequest request) {
        List<LearningExerciseDto> lessons = adminLessonService.batchImport(
                request.getTopicId(), request.getChannelYoutubeId(), request.getLessons());
        return ResponseEntity.status(HttpStatus.CREATED).body(lessons);
    }

    /** PUT /api/admin/lessons/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<LearningExerciseDto> updateLesson(
            @PathVariable Long id,
            @RequestBody UpdateLessonRequest request) {
        return ResponseEntity.ok(adminLessonService.updateLesson(id, request.getTitle(), request.getLevel()));
    }

    /** DELETE /api/admin/lessons/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id) {
        adminLessonService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    // ── Transcript endpoints ──────────────────────────────────────────────────

    /** POST /api/admin/lessons/{lessonId}/transcript — replace all segments */
    @PostMapping("/{lessonId}/transcript")
    public ResponseEntity<List<ExerciseModuleDto>> saveTranscript(
            @PathVariable Long lessonId,
            @RequestBody List<SaveModuleRequest> segments) {
        return ResponseEntity.ok(adminTranscriptService.saveTranscript(lessonId, segments));
    }

    /** GET /api/admin/lessons/{lessonId}/transcript */
    @GetMapping("/{lessonId}/transcript")
    public ResponseEntity<List<ExerciseModuleDto>> getTranscript(@PathVariable Long lessonId) {
        return ResponseEntity.ok(adminTranscriptService.getTranscript(lessonId));
    }

    @PostMapping(value = "/{lessonId}/transcript/srt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ExerciseModuleDto>> importTranscriptSrt(
            @PathVariable Long lessonId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(adminTranscriptService.importSrt(lessonId, file));
    }

    @GetMapping(value = "/{lessonId}/transcript/srt", produces = "application/x-subrip")
    public ResponseEntity<byte[]> exportTranscriptSrt(@PathVariable Long lessonId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"lesson-" + lessonId + "-bilingual.srt\"")
                .contentType(MediaType.parseMediaType("application/x-subrip; charset=UTF-8"))
                .body(adminTranscriptService.exportSrt(lessonId));
    }

    /** PUT /api/admin/lessons/{lessonId}/transcript - update existing segments in place */
    @PutMapping("/{lessonId}/transcript")
    public ResponseEntity<List<ExerciseModuleDto>> updateTranscript(
            @PathVariable Long lessonId,
            @RequestBody List<AdminTranscriptSegmentRequest> segments) {
        return ResponseEntity.ok(adminTranscriptService.updateTranscript(lessonId, segments));
    }

    /** DELETE /api/admin/lessons/{lessonId}/transcript */
    @DeleteMapping("/{lessonId}/transcript")
    public ResponseEntity<Void> deleteTranscript(@PathVariable Long lessonId) {
        adminTranscriptService.deleteTranscript(lessonId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleImportFailure(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}

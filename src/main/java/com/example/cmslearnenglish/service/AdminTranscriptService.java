package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.AdminTranscriptSegmentRequest;
import com.example.cmslearnenglish.dto.ExerciseModuleDto;
import com.example.cmslearnenglish.dto.SaveModuleRequest;
import com.example.cmslearnenglish.entity.ExerciseModule;
import com.example.cmslearnenglish.entity.YoutubeModuleExtension;
import com.example.cmslearnenglish.repository.ExerciseModuleRepository;
import com.example.cmslearnenglish.repository.LearningExerciseRepository;
import com.example.cmslearnenglish.repository.YoutubeExerciseExtensionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AdminTranscriptService {

    private static final Pattern SRT_TIME_RANGE = Pattern.compile(
            "(\\d{2}:\\d{2}:\\d{2}[,.]\\d{3})\\s*-->\\s*(\\d{2}:\\d{2}:\\d{2}[,.]\\d{3})"
    );

    private final YoutubeExerciseService youtubeExerciseService;
    private final LearningExerciseRepository exerciseRepository;
    private final YoutubeExerciseExtensionRepository extensionRepository;
    private final ExerciseModuleRepository moduleRepository;

    @Transactional
    public List<ExerciseModuleDto> saveTranscript(Long lessonId, List<SaveModuleRequest> requests) {
        // lessonId = LearningExercise.id, lấy videoId từ extension rồi delegate
        String videoId = extensionRepository.findByLearningExerciseId(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonId))
                .getVideoId();
        youtubeExerciseService.saveModules(videoId, requests);
        return getTranscript(lessonId);
    }

    @Transactional(readOnly = true)
    public List<ExerciseModuleDto> getTranscript(Long lessonId) {
        if (!exerciseRepository.existsById(lessonId)) {
            throw new IllegalArgumentException("Lesson not found: " + lessonId);
        }
        return youtubeExerciseService.getModules(lessonId, 0, Integer.MAX_VALUE);
    }

    @Transactional
    public List<ExerciseModuleDto> updateTranscript(
            Long lessonId,
            List<AdminTranscriptSegmentRequest> requests) {
        if (!exerciseRepository.existsById(lessonId)) {
            throw new IllegalArgumentException("Lesson not found: " + lessonId);
        }

        for (int index = 0; index < requests.size(); index++) {
            AdminTranscriptSegmentRequest request = requests.get(index);
            validateSegment(request, index);

            ExerciseModule module = moduleRepository.findById(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Transcript segment not found: " + request.getId()));
            if (!lessonId.equals(module.getLearningExercise().getId())) {
                throw new IllegalArgumentException(
                        "Transcript segment does not belong to lesson: " + request.getId());
            }

            YoutubeModuleExtension extension = module.getYoutubeModuleExtension();
            if (extension == null) {
                throw new IllegalArgumentException(
                        "YouTube transcript data not found for segment: " + request.getId());
            }

            extension.setTimeStartMs(request.getTimeStartMs());
            extension.setTimeEndMs(request.getTimeEndMs());
            extension.setCorrectAnswer(request.getContent().trim());
            extension.setVietnameseText(normalizeOptionalText(request.getVietnameseText()));
        }

        return getTranscript(lessonId);
    }

    @Transactional
    public List<ExerciseModuleDto> importSrt(Long lessonId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("SRT file is required");
        }
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read SRT file", exception);
        }
        return saveTranscript(lessonId, parseSrt(content));
    }

    @Transactional(readOnly = true)
    public byte[] exportSrt(Long lessonId) {
        List<ExerciseModuleDto> segments = getTranscript(lessonId);
        StringBuilder srt = new StringBuilder();
        for (int index = 0; index < segments.size(); index++) {
            ExerciseModuleDto segment = segments.get(index);
            srt.append(index + 1).append("\r\n");
            srt.append(formatSrtTime(segment.getTimeStartMs()))
                    .append(" --> ")
                    .append(formatSrtTime(segment.getTimeEndMs()))
                    .append("\r\n");
            srt.append("EN: ").append(nullToEmpty(segment.getContent())).append("\r\n");
            if (segment.getVietnameseText() != null && !segment.getVietnameseText().isBlank()) {
                srt.append("VI: ").append(segment.getVietnameseText().trim()).append("\r\n");
            }
            srt.append("\r\n");
        }
        return ("\uFEFF" + srt).getBytes(StandardCharsets.UTF_8);
    }

    private List<SaveModuleRequest> parseSrt(String content) {
        String normalized = stripBom(content)
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("SRT file is empty");
        }

        String[] blocks = normalized.split("\\n\\s*\\n");
        List<SaveModuleRequest> segments = new ArrayList<>();
        for (int index = 0; index < blocks.length; index++) {
            SaveModuleRequest segment = parseSrtBlock(blocks[index], index + 1);
            if (segment != null) {
                segments.add(segment);
            }
        }
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("SRT file has no valid subtitle segments");
        }
        return segments;
    }

    private SaveModuleRequest parseSrtBlock(String block, int row) {
        List<String> lines = block.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        if (lines.isEmpty()) {
            return null;
        }

        int timeLineIndex = -1;
        Matcher matcher = null;
        for (int index = 0; index < lines.size(); index++) {
            matcher = SRT_TIME_RANGE.matcher(lines.get(index));
            if (matcher.matches()) {
                timeLineIndex = index;
                break;
            }
        }
        if (timeLineIndex < 0 || matcher == null) {
            throw new IllegalArgumentException("Missing SRT time range at block " + row);
        }

        Integer startMs = parseSrtTime(matcher.group(1), row);
        Integer endMs = parseSrtTime(matcher.group(2), row);
        if (endMs <= startMs) {
            throw new IllegalArgumentException("End time must be greater than start time at block " + row);
        }

        List<String> textLines = lines.subList(timeLineIndex + 1, lines.size());
        ParsedSubtitleText text = parseSubtitleText(textLines);
        if (text.english().isBlank()) {
            throw new IllegalArgumentException("English subtitle is required at block " + row);
        }
        return new SaveModuleRequest(startMs, endMs, text.english(), text.vietnamese());
    }

    private ParsedSubtitleText parseSubtitleText(List<String> lines) {
        List<String> english = new ArrayList<>();
        List<String> vietnamese = new ArrayList<>();
        boolean vietnameseSection = false;

        for (String line : lines) {
            String lower = line.toLowerCase(java.util.Locale.ROOT);
            if (line.equals("---")) {
                vietnameseSection = true;
                continue;
            }
            if (lower.startsWith("en:")) {
                english.add(line.substring(3).trim());
                continue;
            }
            if (lower.startsWith("vi:")) {
                vietnamese.add(line.substring(3).trim());
                vietnameseSection = true;
                continue;
            }
            if (vietnameseSection || !english.isEmpty()) {
                vietnamese.add(line);
            } else {
                english.add(line);
            }
        }

        return new ParsedSubtitleText(
                String.join("\n", english).trim(),
                normalizeOptionalText(String.join("\n", vietnamese))
        );
    }

    private Integer parseSrtTime(String value, int row) {
        String[] parts = value.replace(',', '.').split("[:.]");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid SRT time at block " + row);
        }
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            int millis = Integer.parseInt(parts[3]);
            return hours * 3_600_000 + minutes * 60_000 + seconds * 1_000 + millis;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid SRT time at block " + row);
        }
    }

    private String formatSrtTime(Integer milliseconds) {
        int value = milliseconds == null ? 0 : Math.max(milliseconds, 0);
        int hours = value / 3_600_000;
        int minutes = (value % 3_600_000) / 60_000;
        int seconds = (value % 60_000) / 1_000;
        int millis = value % 1_000;
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis);
    }

    private String stripBom(String value) {
        return value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF' ? value.substring(1) : value;
    }

    private void validateSegment(AdminTranscriptSegmentRequest request, int index) {
        int row = index + 1;
        if (request.getId() == null) {
            throw new IllegalArgumentException("Missing segment ID at row " + row);
        }
        if (request.getTimeStartMs() == null || request.getTimeStartMs() < 0) {
            throw new IllegalArgumentException("Start time must be at least 0 at row " + row);
        }
        if (request.getTimeEndMs() == null || request.getTimeEndMs() <= request.getTimeStartMs()) {
            throw new IllegalArgumentException("End time must be greater than start time at row " + row);
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException("English subtitle is required at row " + row);
        }
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    @Transactional
    public void deleteTranscript(Long lessonId) {
        String videoId = extensionRepository.findByLearningExerciseId(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found: " + lessonId))
                .getVideoId();
        youtubeExerciseService.saveModules(videoId, List.of());
    }

    private record ParsedSubtitleText(String english, String vietnamese) {
    }
}

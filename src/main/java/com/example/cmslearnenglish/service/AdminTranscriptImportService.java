package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.LearningExerciseDto;
import com.example.cmslearnenglish.entity.LearningExercise;
import com.example.cmslearnenglish.entity.YoutubeExerciseExtension;
import com.example.cmslearnenglish.repository.YoutubeExerciseExtensionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTranscriptImportService {

    private final ObjectMapper objectMapper;
    private final YoutubeExerciseExtensionRepository extensionRepository;
    private final YoutubeExerciseService youtubeExerciseService;

    @Value("${python.command:python}")
    private String pythonCommand;

    @Value("${python.script.path:scripts/download_transcript.py}")
    private String scriptPath;

    @Value("${python.script.timeout:120}")
    private int timeoutSeconds;

    @Value("${python.script.working-dir:}")
    private String workingDir;

    @Value("${transcript.import.backend-api-url:http://localhost:8081}")
    private String backendApiUrl;

    public LearningExerciseDto importByVideoId(String videoId) {
        JsonNode result = runImportScript(videoId);
        validateScriptResult(videoId, result);

        YoutubeExerciseExtension extension = extensionRepository.findByVideoId(videoId)
                .orElseThrow(() -> new IllegalStateException("Script completed but lesson was not found in database: " + videoId));
        LearningExercise exercise = extension.getLearningExercise();
        return youtubeExerciseService.toExerciseDtoPublic(exercise, extension);
    }

    private JsonNode runImportScript(String videoId) {
        try {
            File workDirectory = resolveWorkingDirectory();
            ProcessBuilder processBuilder = new ProcessBuilder(pythonCommand, scriptPath, videoId);
            processBuilder.directory(workDirectory);
            processBuilder.redirectErrorStream(true);
            Map<String, String> environment = processBuilder.environment();
            environment.put("BACKEND_API_URL", backendApiUrl);

            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Transcript import timed out after " + timeoutSeconds + " seconds");
            }

            String rawOutput = output.toString();
            String json = extractJson(rawOutput, workDirectory);
            JsonNode result = objectMapper.readTree(json);
            if (process.exitValue() != 0 && !result.path("success").asBoolean(false)) {
                throw new IllegalArgumentException(result.path("message").asText("Transcript import failed"));
            }
            return result;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to import transcript for video {}", videoId, ex);
            throw new IllegalStateException("Failed to run transcript import script: " + ex.getMessage(), ex);
        }
    }

    private void validateScriptResult(String videoId, JsonNode result) {
        if (!result.path("success").asBoolean(false)) {
            throw new IllegalArgumentException(result.path("message").asText("Transcript import failed"));
        }

        JsonNode apiStatus = result.path("api_save_status");
        if (apiStatus.isMissingNode() || !apiStatus.path("success").asBoolean(false)) {
            String message = apiStatus.path("message").asText("Transcript downloaded but could not be saved to backend");
            throw new IllegalStateException(message);
        }

        log.info("Imported transcript for video {} with {} segments",
                videoId, apiStatus.path("segments_saved").asInt(0));
    }

    private File resolveWorkingDirectory() {
        if (workingDir != null && !workingDir.isBlank()) {
            return new File(workingDir);
        }
        return new File(System.getProperty("user.dir"));
    }

    private String extractJson(String output, File workDirectory) {
        int start = output.indexOf('{');
        int end = output.lastIndexOf('}');
        if (start < 0 || end < start) {
            String compactOutput = output == null || output.isBlank()
                    ? "(empty output)"
                    : output.strip();
            throw new IllegalStateException(
                    "Transcript script did not return JSON output. Command: "
                            + pythonCommand + " " + scriptPath
                            + ". Working directory: " + workDirectory.getAbsolutePath()
                            + ". Output: " + compactOutput);
        }
        return output.substring(start, end + 1);
    }
}

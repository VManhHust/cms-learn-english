package com.example.cmslearnenglish.service.youtube;

import com.example.cmslearnenglish.dto.youtube.internal.TranscriptSegment;
import com.example.cmslearnenglish.exception.youtube.NoSubtitlesAvailableException;
import com.example.cmslearnenglish.exception.youtube.YoutubeServiceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PythonTranscriptDownloader {

    private final ObjectMapper objectMapper;
    private final String pythonCommand;
    private final String scriptPath;
    private final int timeoutSeconds;

    public PythonTranscriptDownloader(
            ObjectMapper objectMapper,
            @Value("${python.command:python3}") String pythonCommand,
            @Value("${python.script.path:scripts/download_transcript.py}") String scriptPath,
            @Value("${python.script.timeout:30}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.pythonCommand = pythonCommand;
        this.scriptPath = scriptPath;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Downloads transcript for a YouTube video using Python script
     * 
     * @param videoId The YouTube video ID
     * @return List of TranscriptSegment objects
     * @throws NoSubtitlesAvailableException if no subtitles are available
     * @throws YoutubeServiceUnavailableException if there's an error
     */
    public List<TranscriptSegment> downloadTranscript(String videoId) {
        log.info("Downloading transcript using Python script for video: {}", videoId);

        try {
            // Build command
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonCommand,
                    scriptPath,
                    videoId,
                    "en", "en-GB", "en-US"
            );

            // Set working directory to project root
            File projectRoot = new File(System.getProperty("user.dir"));
            processBuilder.directory(projectRoot);
            processBuilder.redirectErrorStream(true);

            // Start process
            Process process = processBuilder.start();

            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Wait for completion with timeout
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                log.error("Python script timed out for video: {}", videoId);
                throw new YoutubeServiceUnavailableException("Transcript download timed out", null);
            }

            int exitCode = process.exitValue();
            String jsonOutput = output.toString().trim();

            log.debug("Python script output: {}", jsonOutput);

            // Parse JSON response
            JsonNode response = objectMapper.readTree(jsonOutput);
            boolean success = response.get("success").asBoolean();

            if (!success) {
                String error = response.get("error").asText();
                String message = response.get("message").asText();
                
                log.error("Python script failed for video {}: {} - {}", videoId, error, message);

                if ("NO_SUBTITLES_AVAILABLE".equals(error) || 
                    "TRANSCRIPTS_DISABLED".equals(error)) {
                    throw new NoSubtitlesAvailableException(videoId);
                }

                throw new YoutubeServiceUnavailableException(message, null);
            }

            // Parse segments
            List<TranscriptSegment> segments = new ArrayList<>();
            JsonNode segmentsNode = response.get("segments");
            
            if (segmentsNode != null && segmentsNode.isArray()) {
                for (JsonNode segmentNode : segmentsNode) {
                    TranscriptSegment segment = TranscriptSegment.builder()
                            .id(segmentNode.get("id").asInt())
                            .startMs(segmentNode.get("t_start_ms").asInt())
                            .endMs(segmentNode.get("t_end_ms").asInt())
                            .text(segmentNode.get("caption").asText())
                            .build();
                    segments.add(segment);
                }
            }

            log.info("Successfully downloaded {} transcript segments for video: {}", 
                    segments.size(), videoId);
            
            return segments;

        } catch (NoSubtitlesAvailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error running Python script for video: {}", videoId, e);
            throw new YoutubeServiceUnavailableException("Failed to download transcript", e);
        }
    }
}

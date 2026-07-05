package com.example.cmslearnenglish.service.youtube;

import com.example.cmslearnenglish.dto.youtube.internal.TranscriptSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TranscriptParser {

    // SRT format pattern: sequence number, timestamp, text
    private static final Pattern SRT_TIMESTAMP_PATTERN = Pattern.compile(
        "(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})"
    );

    /**
     * Parses SRT format subtitle content into a list of TranscriptSegment objects
     * @param srtContent The SRT format subtitle content
     * @return List of TranscriptSegment objects with sequential IDs starting from 0
     */
    public List<TranscriptSegment> parseSrt(String srtContent) {
        if (srtContent == null || srtContent.isBlank()) {
            log.warn("Empty SRT content provided");
            return List.of();
        }

        List<TranscriptSegment> segments = new ArrayList<>();
        String[] blocks = srtContent.split("\\n\\s*\\n"); // Split by double newline
        
        int segmentId = 0;
        for (String block : blocks) {
            if (block.isBlank()) {
                continue;
            }

            String[] lines = block.split("\\n");
            if (lines.length < 2) {
                continue; // Skip malformed blocks
            }

            // Find the timestamp line (usually line 1, but could be line 0 if no sequence number)
            String timestampLine = null;
            int textStartIndex = 2;
            
            for (int i = 0; i < Math.min(2, lines.length); i++) {
                if (SRT_TIMESTAMP_PATTERN.matcher(lines[i]).find()) {
                    timestampLine = lines[i];
                    textStartIndex = i + 1;
                    break;
                }
            }

            if (timestampLine == null) {
                log.debug("No timestamp found in block, skipping");
                continue;
            }

            Matcher matcher = SRT_TIMESTAMP_PATTERN.matcher(timestampLine);
            if (!matcher.find()) {
                log.debug("Failed to parse timestamp: {}", timestampLine);
                continue;
            }

            try {
                // Parse start time
                int startHours = Integer.parseInt(matcher.group(1));
                int startMinutes = Integer.parseInt(matcher.group(2));
                int startSeconds = Integer.parseInt(matcher.group(3));
                int startMillis = Integer.parseInt(matcher.group(4));
                int startMs = timeToMillis(startHours, startMinutes, startSeconds, startMillis);

                // Parse end time
                int endHours = Integer.parseInt(matcher.group(5));
                int endMinutes = Integer.parseInt(matcher.group(6));
                int endSeconds = Integer.parseInt(matcher.group(7));
                int endMillis = Integer.parseInt(matcher.group(8));
                int endMs = timeToMillis(endHours, endMinutes, endSeconds, endMillis);

                // Extract text (all remaining lines)
                StringBuilder textBuilder = new StringBuilder();
                for (int i = textStartIndex; i < lines.length; i++) {
                    if (i > textStartIndex) {
                        textBuilder.append(" ");
                    }
                    textBuilder.append(lines[i].trim());
                }
                String text = textBuilder.toString();

                if (!text.isBlank()) {
                    TranscriptSegment segment = TranscriptSegment.builder()
                            .id(segmentId++)
                            .startMs(startMs)
                            .endMs(endMs)
                            .text(text)
                            .build();
                    segments.add(segment);
                }
            } catch (NumberFormatException e) {
                log.warn("Failed to parse timestamp numbers: {}", timestampLine, e);
            }
        }

        log.info("Parsed {} segments from SRT content", segments.size());
        return segments;
    }

    /**
     * Converts time components to milliseconds
     * @param hours Hours component
     * @param minutes Minutes component
     * @param seconds Seconds component
     * @param millis Milliseconds component
     * @return Total time in milliseconds
     */
    private int timeToMillis(int hours, int minutes, int seconds, int millis) {
        return (hours * 3600 + minutes * 60 + seconds) * 1000 + millis;
    }
}

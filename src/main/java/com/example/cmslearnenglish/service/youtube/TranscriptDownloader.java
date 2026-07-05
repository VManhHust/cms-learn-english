package com.example.cmslearnenglish.service.youtube;

import com.example.cmslearnenglish.dto.youtube.internal.TranscriptSegment;
import com.example.cmslearnenglish.exception.youtube.NoSubtitlesAvailableException;
import com.example.cmslearnenglish.exception.youtube.YoutubeServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranscriptDownloader {

    private final PythonTranscriptDownloader pythonTranscriptDownloader;

    /**
     * Downloads and parses transcript for a YouTube video using Python script
     * @param videoId The YouTube video ID
     * @return List of TranscriptSegment objects
     * @throws NoSubtitlesAvailableException if no subtitles are available
     * @throws YoutubeServiceUnavailableException if there's a download error
     */
    public List<TranscriptSegment> downloadTranscript(String videoId) {
        log.info("Downloading transcript for video ID: {}", videoId);
        return pythonTranscriptDownloader.downloadTranscript(videoId);
    }
}

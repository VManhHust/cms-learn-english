package com.example.cmslearnenglish.service.youtube;

import com.example.cmslearnenglish.exception.youtube.InvalidYoutubeUrlException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class YoutubeUrlValidator {

    private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
        "^(https?://)?(www\\.)?(youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11}).*$"
    );

    /**
     * Validates if the given URL is a valid YouTube video URL
     * @param url The URL to validate
     * @return true if valid, false otherwise
     */
    public boolean isValid(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        return YOUTUBE_URL_PATTERN.matcher(url).matches();
    }

    /**
     * Extracts the 11-character video ID from a YouTube URL
     * @param url The YouTube URL
     * @return The video ID
     * @throws InvalidYoutubeUrlException if the URL is invalid
     */
    public String extractVideoId(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidYoutubeUrlException(url);
        }

        Matcher matcher = YOUTUBE_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            log.warn("Invalid YouTube URL format: {}", url);
            throw new InvalidYoutubeUrlException(url);
        }

        String videoId = matcher.group(4);
        log.debug("Extracted video ID: {} from URL: {}", videoId, url);
        return videoId;
    }
}

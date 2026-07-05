package com.example.cmslearnenglish.exception.youtube;

public class YoutubeTranscriptException extends RuntimeException {
    public YoutubeTranscriptException(String message) {
        super(message);
    }

    public YoutubeTranscriptException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.example.cmslearnenglish.exception.youtube;

public class YoutubeServiceUnavailableException extends YoutubeTranscriptException {
    public YoutubeServiceUnavailableException(String message, Throwable cause) {
        super("YouTube service unavailable: " + message, cause);
    }
}

package com.example.cmslearnenglish.exception.youtube;

public class VideoNotFoundException extends YoutubeTranscriptException {
    public VideoNotFoundException(String videoId) {
        super("Video not found: " + videoId);
    }
}

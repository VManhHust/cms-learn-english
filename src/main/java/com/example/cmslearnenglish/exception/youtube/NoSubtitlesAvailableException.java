package com.example.cmslearnenglish.exception.youtube;

public class NoSubtitlesAvailableException extends YoutubeTranscriptException {
    public NoSubtitlesAvailableException(String videoId) {
        super("No subtitles available for video: " + videoId);
    }
}

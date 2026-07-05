package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.TranscriptResponse;

public interface TranscriptService {
    TranscriptResponse getTranscriptByLearningTopicId(Long learningTopicId);
}

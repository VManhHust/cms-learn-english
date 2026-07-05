package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.TranscriptResponse;
import com.example.cmslearnenglish.dto.TranscriptSegmentDto;
import com.example.cmslearnenglish.entity.YoutubeModuleExtension;
import com.example.cmslearnenglish.exception.TranscriptNotFoundException;
import com.example.cmslearnenglish.repository.LearningTopicRepository;
import com.example.cmslearnenglish.repository.YoutubeModuleExtensionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TranscriptServiceImpl implements TranscriptService {

    private final LearningTopicRepository learningTopicRepository;
    private final YoutubeModuleExtensionRepository youtubeModuleExtensionRepository;

    @Override
    @Transactional(readOnly = true)
    public TranscriptResponse getTranscriptByLearningTopicId(Long learningTopicId) {
        // Validate topic exists
        if (!learningTopicRepository.existsById(learningTopicId)) {
            throw new TranscriptNotFoundException(learningTopicId);
        }

        // Query segments sorted by startTimeMs
        List<YoutubeModuleExtension> segments = youtubeModuleExtensionRepository
                .findByLearningTopicIdOrderByTimeStartMsAsc(learningTopicId);

        // Map to DTOs
        List<TranscriptSegmentDto> segmentDtos = segments.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        // Return response with metadata
        return TranscriptResponse.builder()
                .learningTopicId(learningTopicId)
                .segments(segmentDtos)
                .totalSegments(segmentDtos.size())
                .build();
    }

    private TranscriptSegmentDto mapToDto(YoutubeModuleExtension entity) {
        return TranscriptSegmentDto.builder()
                .id(entity.getId())
                .startTimeMs(entity.getTimeStartMs())
                .endTimeMs(entity.getTimeEndMs())
                .text(entity.getCorrectAnswer())
                .vietnameseText(entity.getVietnameseText())
                .build();
    }
}

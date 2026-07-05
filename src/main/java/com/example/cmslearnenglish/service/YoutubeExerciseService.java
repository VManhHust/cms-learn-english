package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.*;
import com.example.cmslearnenglish.entity.*;
import com.example.cmslearnenglish.entity.enums.ExerciseModuleType;
import com.example.cmslearnenglish.entity.enums.LearningExerciseType;
import com.example.cmslearnenglish.entity.enums.LearningTopicType;
import com.example.cmslearnenglish.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeExerciseService {

    private final YoutubeChannelRepository channelRepository;
    private final LearningTopicRepository topicRepository;
    private final LearningExerciseRepository exerciseRepository;
    private final YoutubeExerciseExtensionRepository extensionRepository;
    private final ExerciseModuleRepository moduleRepository;

    // ── Channel ──────────────────────────────────────────────────────────────

    public List<YoutubeChannelDto> getOutstandingChannels(int maxCount) {
        return channelRepository.findOutstandingChannels(maxCount)
                .stream().map(this::toChannelDto).toList();
    }

    public Page<YoutubeChannelDto> getChannels(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return channelRepository.findAll(pageable).map(this::toChannelDto);
    }

    public YoutubeChannelDto getChannelById(Long id) {
        return toChannelDto(channelRepository.findById(id).orElseThrow());
    }

    @Transactional
    public YoutubeChannelDto saveChannel(YoutubeChannelDto dto) {
        YoutubeChannel channel = channelRepository.findByChannelYoutubeId(dto.getChannelYoutubeId())
                .orElse(YoutubeChannel.builder().channelYoutubeId(dto.getChannelYoutubeId()).build());
        channel.setChannelName(dto.getChannelName());
        channel.setChannelImgUrl(dto.getChannelImgUrl());
        channel.setChannelDescription(dto.getChannelDescription());
        channel.setSubscriberCount(dto.getSubscriberCount());
        return toChannelDto(channelRepository.save(channel));
    }

    // ── Exercise ─────────────────────────────────────────────────────────────

    public Page<LearningExerciseDto> getExercisesByChannel(Long channelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return extensionRepository.findByChannelId(channelId, pageable)
                .map(ext -> toExerciseDto(ext.getLearningExercise(), ext));
    }

    public LearningExerciseDto getExerciseById(Long exerciseId) {
        LearningExercise exercise = exerciseRepository.findById(exerciseId).orElseThrow();
        return toExerciseDto(exercise, exercise.getYoutubeExerciseExtension());
    }

    @Transactional
    public void saveExercises(String channelYoutubeId, List<SaveExerciseRequest> requests) {
        YoutubeChannel channel = channelRepository.findByChannelYoutubeId(channelYoutubeId).orElseThrow();
        LearningTopic topic = topicRepository.findByType(LearningTopicType.YOUTUBE).orElseThrow();
        Set<YoutubeExerciseExtension> extensions = channel.getExtensions();

        requests.forEach(req -> {
            Optional<YoutubeExerciseExtension> existing = extensions.stream()
                    .filter(e -> e.getVideoId().equals(req.getVideoId())).findFirst();
            if (existing.isPresent()) {
                YoutubeExerciseExtension ext = existing.get();
                ext.setThumbnailUrl(req.getThumbnailUrl());
                ext.setDurationSeconds(req.getDurationSeconds());
                ext.getLearningExercise().setTitle(req.getTitle());
                ext.getLearningExercise().setVocabularyLevel(req.getVocabularyLevel());
                log.info("Updated exercise for videoId {}", req.getVideoId());
            } else {
                LearningExercise exercise = LearningExercise.builder()
                        .uuid(req.getVideoId())
                        .type(LearningExerciseType.YOUTUBE_VIDEO)
                        .title(req.getTitle())
                        .vocabularyLevel(req.getVocabularyLevel())
                        .moduleCount(0)
                        .learningTopic(topic)
                        .build();
                YoutubeExerciseExtension ext = YoutubeExerciseExtension.builder()
                        .videoId(req.getVideoId())
                        .thumbnailUrl(req.getThumbnailUrl())
                        .durationSeconds(req.getDurationSeconds())
                        .youtubeChannel(channel)
                        .learningExercise(exercise)
                        .build();
                extensions.add(ext);
                log.info("Created new exercise for videoId {}", req.getVideoId());
            }
        });
    }

    // ── Module (transcript segments) ─────────────────────────────────────────

    public List<ExerciseModuleDto> getModules(Long exerciseId, int offset, int limit) {
        Pageable pageable = PageRequest.of(0, limit).withPage(offset / limit);
        return moduleRepository.findByExerciseIdOrderByTimeStart(exerciseId, pageable)
                .stream().map(this::toModuleDto).toList();
    }

    @Transactional
    public void saveModules(String videoId, List<SaveModuleRequest> requests) {
        YoutubeExerciseExtension ext = extensionRepository.findByVideoId(videoId).orElseThrow();
        LearningExercise exercise = ext.getLearningExercise();

        Set<ExerciseModule> existing = exercise.getExerciseModules();
        moduleRepository.deleteAll(existing);
        existing.clear();

        requests.forEach(req -> {
            YoutubeModuleExtension moduleExt = YoutubeModuleExtension.builder()
                    .timeStartMs(req.getTimeStartMs())
                    .timeEndMs(req.getTimeEndMs())
                    .correctAnswer(req.getContent())
                    .vietnameseText(req.getVietnameseText())
                    .build();
            ExerciseModule module = ExerciseModule.builder()
                    .type(ExerciseModuleType.YOUTUBE)
                    .learningExercise(exercise)
                    .youtubeModuleExtension(moduleExt)
                    .build();
            existing.add(module);
        });

        exercise.setModuleCount(existing.size());
        log.info("Saved {} modules for videoId {}", existing.size(), videoId);
    }

    // ── Public mapper (used by other services) ────────────────────────────────

    public LearningExerciseDto toExerciseDtoPublic(LearningExercise e, YoutubeExerciseExtension ext) {
        return toExerciseDto(e, ext);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private YoutubeChannelDto toChannelDto(YoutubeChannel c) {
        return new YoutubeChannelDto(c.getId(), c.getChannelYoutubeId(), c.getChannelName(),
                c.getChannelImgUrl(), c.getChannelDescription(), c.getSubscriberCount());
    }

    private LearningExerciseDto toExerciseDto(LearningExercise e, YoutubeExerciseExtension ext) {
        YoutubeChannelDto channelDto = ext.getYoutubeChannel() != null
                ? toChannelDto(ext.getYoutubeChannel()) : null;
        Long topicId = e.getLearningTopic() != null ? e.getLearningTopic().getId() : null;
        String topicName = e.getLearningTopic() != null ? e.getLearningTopic().getTopicName() : null;
        return new LearningExerciseDto(e.getId(), e.getUuid(), e.getType(), e.getTitle(),
                e.getModuleCount(), e.getVocabularyLevel(),
                ext.getVideoId(), ext.getThumbnailUrl(), ext.getDurationSeconds(), channelDto,
                topicId, topicName, e.isPremium());
    }

    private ExerciseModuleDto toModuleDto(ExerciseModule m) {
        YoutubeModuleExtension ext = m.getYoutubeModuleExtension();
        return new ExerciseModuleDto(m.getId(), ext.getTimeStartMs(), ext.getTimeEndMs(), ext.getCorrectAnswer(), ext.getVietnameseText());
    }
}

package com.example.cmslearnenglish.controller;

import com.example.cmslearnenglish.dto.ExerciseModuleDto;
import com.example.cmslearnenglish.dto.LearningExerciseDto;
import com.example.cmslearnenglish.dto.SaveExerciseRequest;
import com.example.cmslearnenglish.dto.SaveModuleRequest;
import com.example.cmslearnenglish.dto.YoutubeChannelDto;
import com.example.cmslearnenglish.service.YoutubeExerciseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topic/youtube")
@RequiredArgsConstructor
public class YoutubeExerciseController {

    private final YoutubeExerciseService service;

    @GetMapping("/channel/outstanding")
    public List<YoutubeChannelDto> getOutstandingChannels(@RequestParam(defaultValue = "5") int maxCount) {
        return service.getOutstandingChannels(maxCount);
    }

    @GetMapping("/channel/all")
    public Page<YoutubeChannelDto> getChannels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getChannels(page, size);
    }

    @GetMapping("/channel/{channelId}")
    public YoutubeChannelDto getChannelById(@PathVariable Long channelId) {
        return service.getChannelById(channelId);
    }

    @PostMapping("/channel/save")
    public YoutubeChannelDto saveChannel(@RequestBody YoutubeChannelDto dto) {
        return service.saveChannel(dto);
    }

    @GetMapping("/exercises/channel/{channelId}")
    public Page<LearningExerciseDto> getExercisesByChannel(
            @PathVariable Long channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getExercisesByChannel(channelId, page, size);
    }

    @GetMapping("/exercises/{exerciseId}")
    public LearningExerciseDto getExerciseById(@PathVariable Long exerciseId) {
        return service.getExerciseById(exerciseId);
    }

    @PostMapping("/exercises/save/by-channel-youtube-id/{channelYoutubeId}")
    public ResponseEntity<Void> saveExercises(
            @PathVariable String channelYoutubeId,
            @RequestBody List<SaveExerciseRequest> requests) {
        service.saveExercises(channelYoutubeId, requests);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/exercises/{exerciseId}/modules")
    public List<ExerciseModuleDto> getModules(
            @PathVariable Long exerciseId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        return service.getModules(exerciseId, offset, limit);
    }

    @PostMapping("/exercises/content/save/by-video-id/{videoId}")
    public ResponseEntity<Void> saveModules(
            @PathVariable String videoId,
            @RequestBody List<SaveModuleRequest> requests) {
        service.saveModules(videoId, requests);
        return ResponseEntity.ok().build();
    }
}

package com.example.cmslearnenglish.service.youtube;

import com.example.cmslearnenglish.dto.youtube.CaptionSegment;
import com.example.cmslearnenglish.dto.youtube.ChannelData;
import com.example.cmslearnenglish.dto.youtube.VideoData;
import com.example.cmslearnenglish.dto.youtube.transform.TransformToSaveChannelDto;
import com.example.cmslearnenglish.dto.youtube.transform.TransformToSaveExerciseDto;
import com.example.cmslearnenglish.dto.youtube.transform.TransformToSaveModuleDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for transforming YouTube transcript data to formats compatible with existing BE-learn-english endpoints
 */
@Slf4j
public class YoutubeDataTransformer {

    /**
     * Transforms ChannelData to format compatible with POST /api/v1/topic/youtube/channel/save
     * Uses medium thumbnail URL for channelImgUrl
     */
    public static TransformToSaveChannelDto toChannelDto(ChannelData channelData) {
        if (channelData == null) {
            return null;
        }

        String channelImgUrl = null;
        if (channelData.getChannelThumbnail() != null && channelData.getChannelThumbnail().getMedium() != null) {
            channelImgUrl = channelData.getChannelThumbnail().getMedium().getUrl();
        }

        TransformToSaveChannelDto dto = new TransformToSaveChannelDto();
        dto.setChannelName(channelData.getChannelName());
        dto.setChannelYoutubeId(channelData.getChannelID());
        dto.setChannelImgUrl(channelImgUrl);
        dto.setChannelDescription(channelData.getChannelDescription());
        dto.setSubscriberCount(channelData.getChannelStatistics() != null 
            ? channelData.getChannelStatistics().getSubscriberCount() 
            : 0L);
        return dto;
    }

    /**
     * Transforms VideoData to format compatible with POST /api/v1/topic/youtube/exercises/save/by-channel-youtube-id/{channelId}
     * Sets vocabularyLevel to null (default)
     */
    public static TransformToSaveExerciseDto toExerciseDto(VideoData videoData) {
        if (videoData == null) {
            return null;
        }

        TransformToSaveExerciseDto dto = new TransformToSaveExerciseDto();
        dto.setVideoId(videoData.getVideoID());
        dto.setTitle(videoData.getTitle());
        dto.setThumbnailUrl(videoData.getThumbnailUrl());
        dto.setDurationSeconds(videoData.getLength());
        dto.setVocabularyLevel(null);  // Default vocabulary level
        return dto;
    }

    /**
     * Transforms list of CaptionSegments to format compatible with POST /api/v1/topic/youtube/exercises/content/save/by-video-id/{videoId}
     */
    public static List<TransformToSaveModuleDto> toModuleDtos(List<CaptionSegment> captions) {
        if (captions == null) {
            return List.of();
        }

        return captions.stream()
                .map(caption -> {
                    TransformToSaveModuleDto dto = new TransformToSaveModuleDto();
                    dto.setTimeStartMs(caption.getT_start_ms());
                    dto.setTimeEndMs(caption.getT_end_ms());
                    dto.setContent(caption.getCaption());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}

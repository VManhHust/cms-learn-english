package com.example.cmslearnenglish.service.youtube;

import com.example.cmslearnenglish.dto.youtube.internal.VideoMetadata;
import com.example.cmslearnenglish.exception.youtube.VideoNotFoundException;
import com.example.cmslearnenglish.exception.youtube.YoutubeServiceUnavailableException;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.videos.VideoDetails;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoMetadataExtractor {

    private final YoutubeDownloader downloader;
    private final YouTube youtubeClient;

    /**
     * Extracts video metadata from YouTube
     * @param videoId The YouTube video ID
     * @return VideoMetadata containing title, videoId, lengthSeconds, channelId, thumbnailUrl
     * @throws VideoNotFoundException if the video is not found or unavailable
     * @throws YoutubeServiceUnavailableException if there's a network or service error
     */
    public VideoMetadata extractMetadata(String videoId) {
        log.info("Extracting metadata for video ID: {}", videoId);

        try {
            RequestVideoInfo request = new RequestVideoInfo(videoId);
            Response<VideoInfo> response = downloader.getVideoInfo(request);

            if (!response.ok()) {
                log.error("Failed to get video info for {}: {}", videoId, response.error());
                throw new VideoNotFoundException(videoId);
            }

            VideoInfo videoInfo = response.data();
            VideoDetails details = videoInfo.details();

            String thumbnailUrl = details.thumbnails().isEmpty() 
                ? null 
                : details.thumbnails().get(0);

            // Get channel ID from YouTube Data API
            String channelId = getChannelIdFromVideo(videoId);

            VideoMetadata metadata = VideoMetadata.builder()
                    .title(details.title())
                    .videoId(details.videoId())
                    .lengthSeconds(details.lengthSeconds())
                    .channelId(channelId)
                    .thumbnailUrl(thumbnailUrl)
                    .build();

            log.info("Successfully extracted metadata for video: {} ({})", metadata.getTitle(), videoId);
            return metadata;

        } catch (VideoNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error extracting video metadata for {}", videoId, e);
            throw new YoutubeServiceUnavailableException("Failed to extract video metadata", e);
        }
    }

    /**
     * Gets channel ID from video ID using YouTube Data API
     */
    private String getChannelIdFromVideo(String videoId) {
        try {
            YouTube.Videos.List request = youtubeClient.videos()
                    .list(List.of("snippet"))
                    .setId(List.of(videoId));

            VideoListResponse response = request.execute();

            if (response.getItems() == null || response.getItems().isEmpty()) {
                log.error("Video not found in YouTube API: {}", videoId);
                throw new VideoNotFoundException(videoId);
            }

            Video video = response.getItems().get(0);
            String channelId = video.getSnippet().getChannelId();
            
            log.debug("Found channel ID {} for video {}", channelId, videoId);
            return channelId;

        } catch (Exception e) {
            log.error("Error getting channel ID for video {}", videoId, e);
            throw new YoutubeServiceUnavailableException("Failed to get channel ID", e);
        }
    }
}

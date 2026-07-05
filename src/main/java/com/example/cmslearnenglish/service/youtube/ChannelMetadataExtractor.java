package com.example.cmslearnenglish.service.youtube;

import com.example.cmslearnenglish.dto.youtube.ChannelStatistics;
import com.example.cmslearnenglish.dto.youtube.ThumbnailInfo;
import com.example.cmslearnenglish.dto.youtube.internal.ChannelMetadata;
import com.example.cmslearnenglish.exception.youtube.YoutubeServiceUnavailableException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.ThumbnailDetails;
import com.google.common.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelMetadataExtractor {

    private final YouTube youtubeClient;
    private final Cache<String, Object> channelMetadataCache;

    /**
     * Extracts channel metadata from YouTube Data API v3 with caching
     * @param channelId The YouTube channel ID
     * @return ChannelMetadata containing all channel information
     * @throws YoutubeServiceUnavailableException if there's an API error
     */
    @SuppressWarnings("unchecked")
    public ChannelMetadata extractMetadata(String channelId) {
        log.info("Extracting metadata for channel ID: {}", channelId);

        // Check cache first
        ChannelMetadata cached = (ChannelMetadata) channelMetadataCache.getIfPresent(channelId);
        if (cached != null) {
            log.info("Cache hit for channel: {}", channelId);
            return cached;
        }

        log.info("Cache miss for channel: {}, fetching from API", channelId);

        try {
            YouTube.Channels.List request = youtubeClient.channels()
                    .list(List.of("snippet", "statistics"))
                    .setId(List.of(channelId));

            ChannelListResponse response = request.execute();

            if (response.getItems() == null || response.getItems().isEmpty()) {
                log.error("Channel not found: {}", channelId);
                throw new YoutubeServiceUnavailableException("Channel not found: " + channelId, null);
            }

            Channel channel = response.getItems().get(0);
            
            // Extract thumbnails
            Map<String, ThumbnailInfo> thumbnails = extractThumbnails(channel.getSnippet().getThumbnails());

            // Extract statistics
            ChannelStatistics statistics = extractStatistics(channel.getStatistics());

            // Get channel handle (customUrl)
            String channelHandle = channel.getSnippet().getCustomUrl();

            ChannelMetadata metadata = ChannelMetadata.builder()
                    .channelId(channel.getId())
                    .channelHandle(channelHandle)
                    .channelName(channel.getSnippet().getTitle())
                    .channelDescription(channel.getSnippet().getDescription())
                    .thumbnails(thumbnails)
                    .statistics(statistics)
                    .build();

            // Cache the result
            channelMetadataCache.put(channelId, metadata);
            log.info("Successfully extracted and cached metadata for channel: {} ({})", 
                    metadata.getChannelName(), channelId);

            return metadata;

        } catch (IOException e) {
            log.error("Error extracting channel metadata for {}", channelId, e);
            throw new YoutubeServiceUnavailableException("Failed to extract channel metadata", e);
        }
    }

    /**
     * Extracts thumbnail information from ThumbnailDetails
     */
    private Map<String, ThumbnailInfo> extractThumbnails(ThumbnailDetails thumbnailDetails) {
        Map<String, ThumbnailInfo> thumbnails = new HashMap<>();

        if (thumbnailDetails.getDefault() != null) {
            thumbnails.put("default", convertThumbnail(thumbnailDetails.getDefault()));
        }
        if (thumbnailDetails.getMedium() != null) {
            thumbnails.put("medium", convertThumbnail(thumbnailDetails.getMedium()));
        }
        if (thumbnailDetails.getHigh() != null) {
            thumbnails.put("high", convertThumbnail(thumbnailDetails.getHigh()));
        }
        if (thumbnailDetails.getStandard() != null) {
            thumbnails.put("standard", convertThumbnail(thumbnailDetails.getStandard()));
        }
        if (thumbnailDetails.getMaxres() != null) {
            thumbnails.put("maxres", convertThumbnail(thumbnailDetails.getMaxres()));
        }

        return thumbnails;
    }

    /**
     * Converts YouTube API Thumbnail to ThumbnailInfo
     */
    private ThumbnailInfo convertThumbnail(Thumbnail thumbnail) {
        return new ThumbnailInfo(
                thumbnail.getUrl(),
                thumbnail.getWidth() != null ? thumbnail.getWidth().intValue() : null,
                thumbnail.getHeight() != null ? thumbnail.getHeight().intValue() : null
        );
    }

    /**
     * Extracts statistics from channel statistics
     */
    private ChannelStatistics extractStatistics(com.google.api.services.youtube.model.ChannelStatistics stats) {
        if (stats == null) {
            return new ChannelStatistics(0L, 0L, true, 0L);
        }

        return new ChannelStatistics(
                bigIntegerToLong(stats.getViewCount()),
                bigIntegerToLong(stats.getSubscriberCount()),
                stats.getHiddenSubscriberCount(),
                bigIntegerToLong(stats.getVideoCount())
        );
    }

    /**
     * Safely converts BigInteger to Long
     */
    private Long bigIntegerToLong(BigInteger value) {
        return value != null ? value.longValue() : 0L;
    }
}

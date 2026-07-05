package com.example.cmslearnenglish.service.youtube;

import com.example.cmslearnenglish.dto.youtube.*;
import com.example.cmslearnenglish.dto.youtube.internal.ChannelMetadata;
import com.example.cmslearnenglish.dto.youtube.internal.TranscriptSegment;
import com.example.cmslearnenglish.dto.youtube.internal.VideoMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeTranscriptService {

    private final VideoMetadataExtractor videoExtractor;
    private final ChannelMetadataExtractor channelExtractor;
    private final TranscriptDownloader transcriptDownloader;
    private final YoutubeUrlValidator urlValidator;

    @Value("${youtube.api.timeout-seconds}")
    private int timeoutSeconds;

    /**
     * Downloads transcripts for multiple YouTube videos
     * @param request Request containing list of YouTube URLs
     * @return Batch response with results for each video
     */
    public YoutubeTranscriptBatchResponse downloadTranscripts(YoutubeTranscriptRequest request) {
        log.info("Processing batch request with {} URLs", request.getUrls().size());

        // Validate and extract video IDs
        List<String> videoIds = new ArrayList<>();
        for (String url : request.getUrls()) {
            try {
                String videoId = urlValidator.extractVideoId(url);
                videoIds.add(videoId);
            } catch (Exception e) {
                log.warn("Invalid URL in batch: {}", url, e);
                // Continue with other URLs
            }
        }

        if (videoIds.isEmpty()) {
            log.warn("No valid URLs found in batch request");
            return new YoutubeTranscriptBatchResponse(List.of(), 0, request.getUrls().size());
        }

        // Process videos in parallel with timeout
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(videoIds.size(), Runtime.getRuntime().availableProcessors())
        );

        List<CompletableFuture<YoutubeTranscriptResponse>> futures = videoIds.stream()
                .map(videoId -> CompletableFuture.supplyAsync(
                        () -> processVideoWithTimeout(videoId),
                        executor
                ))
                .collect(Collectors.toList());

        // Wait for all to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        try {
            allOf.get(timeoutSeconds * videoIds.size(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Batch processing timed out after {} seconds", timeoutSeconds * videoIds.size());
        } catch (Exception e) {
            log.error("Error during batch processing", e);
        } finally {
            executor.shutdown();
        }

        // Collect results
        List<YoutubeTranscriptResponse> results = futures.stream()
                .map(future -> {
                    try {
                        return future.get(1, TimeUnit.SECONDS); // Quick get since already completed
                    } catch (Exception e) {
                        log.error("Failed to get result from future", e);
                        YoutubeTranscriptResponse errorResponse = new YoutubeTranscriptResponse();
                        errorResponse.setSuccess(false);
                        errorResponse.setVideoId(null);
                        errorResponse.setVideo(null);
                        errorResponse.setChannel(null);
                        errorResponse.setErrorMessage("Processing failed: " + e.getMessage());
                        return errorResponse;
                    }
                })
                .collect(Collectors.toList());

        int successCount = (int) results.stream().filter(YoutubeTranscriptResponse::isSuccess).count();
        int failureCount = results.size() - successCount;

        log.info("Batch processing complete: {} success, {} failures", successCount, failureCount);

        return new YoutubeTranscriptBatchResponse(results, successCount, failureCount);
    }

    /**
     * Processes a single video with timeout
     */
    private YoutubeTranscriptResponse processVideoWithTimeout(String videoId) {
        try {
            CompletableFuture<YoutubeTranscriptResponse> future = CompletableFuture.supplyAsync(
                    () -> processVideo(videoId)
            );
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Timeout processing video: {}", videoId);
            YoutubeTranscriptResponse response = new YoutubeTranscriptResponse();
            response.setSuccess(false);
            response.setVideoId(videoId);
            response.setVideo(null);
            response.setChannel(null);
            response.setErrorMessage("Processing timeout after " + timeoutSeconds + " seconds");
            return response;
        } catch (Exception e) {
            log.error("Error processing video: {}", videoId, e);
            YoutubeTranscriptResponse response = new YoutubeTranscriptResponse();
            response.setSuccess(false);
            response.setVideoId(videoId);
            response.setVideo(null);
            response.setChannel(null);
            response.setErrorMessage("Processing error: " + e.getMessage());
            return response;
        }
    }

    /**
     * Processes a single video: extracts metadata, channel info, and transcript
     */
    private YoutubeTranscriptResponse processVideo(String videoId) {
        log.info("Processing video: {}", videoId);

        try {
            // Extract video metadata
            VideoMetadata videoMetadata = videoExtractor.extractMetadata(videoId);

            // Extract channel metadata (with caching)
            ChannelMetadata channelMetadata = channelExtractor.extractMetadata(videoMetadata.getChannelId());

            // Download transcript
            List<TranscriptSegment> transcriptSegments = transcriptDownloader.downloadTranscript(videoId);

            // Transform to response DTOs
            VideoData videoData = transformToVideoData(videoMetadata, transcriptSegments);
            ChannelData channelData = transformToChannelData(channelMetadata);

            log.info("Successfully processed video: {}", videoId);
            YoutubeTranscriptResponse response = new YoutubeTranscriptResponse();
            response.setSuccess(true);
            response.setVideoId(videoId);
            response.setVideo(videoData);
            response.setChannel(channelData);
            response.setErrorMessage(null);
            return response;

        } catch (Exception e) {
            log.error("Failed to process video: {}", videoId, e);
            YoutubeTranscriptResponse response = new YoutubeTranscriptResponse();
            response.setSuccess(false);
            response.setVideoId(videoId);
            response.setVideo(null);
            response.setChannel(null);
            response.setErrorMessage(e.getMessage());
            return response;
        }
    }

    /**
     * Transforms VideoMetadata and TranscriptSegments to VideoData DTO
     */
    private VideoData transformToVideoData(VideoMetadata metadata, List<TranscriptSegment> segments) {
        List<CaptionSegment> captions = segments.stream()
                .map(seg -> {
                    CaptionSegment caption = new CaptionSegment();
                    caption.setId(seg.getId());
                    caption.setT_start_ms(seg.getStartMs());
                    caption.setT_end_ms(seg.getEndMs());
                    caption.setCaption(seg.getText());
                    return caption;
                })
                .collect(Collectors.toList());

        VideoData videoData = new VideoData();
        videoData.setTitle(metadata.getTitle());
        videoData.setVideoID(metadata.getVideoId());
        videoData.setLength(metadata.getLengthSeconds());
        videoData.setChannelID(metadata.getChannelId());
        videoData.setThumbnailUrl(metadata.getThumbnailUrl());
        videoData.setCaptions(captions);
        return videoData;
    }

    /**
     * Transforms ChannelMetadata to ChannelData DTO
     */
    private ChannelData transformToChannelData(ChannelMetadata metadata) {
        ChannelThumbnails thumbnails = new ChannelThumbnails();
        thumbnails.setDefaultThumbnail(metadata.getThumbnails().get("default"));
        thumbnails.setMedium(metadata.getThumbnails().get("medium"));
        thumbnails.setHigh(metadata.getThumbnails().get("high"));
        thumbnails.setStandard(metadata.getThumbnails().get("standard"));
        thumbnails.setMaxres(metadata.getThumbnails().get("maxres"));

        ChannelData channelData = new ChannelData();
        channelData.setChannelID(metadata.getChannelId());
        channelData.setChannelHandle(metadata.getChannelHandle());
        channelData.setChannelName(metadata.getChannelName());
        channelData.setChannelDescription(metadata.getChannelDescription());
        channelData.setChannelThumbnail(thumbnails);
        channelData.setChannelStatistics(metadata.getStatistics());
        return channelData;
    }
}

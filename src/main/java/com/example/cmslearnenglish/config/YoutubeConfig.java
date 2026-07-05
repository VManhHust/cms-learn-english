package com.example.cmslearnenglish.config;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequestInitializer;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class YoutubeConfig {

    @Bean
    public YoutubeDownloader youtubeDownloader(
            @Value("${youtube.downloader.max-retries}") int maxRetries) {
        log.info("Initializing YoutubeDownloader with maxRetries: {}", maxRetries);
        return new YoutubeDownloader();
    }

    @Bean
    public YouTube youtubeClient(
            @Value("${youtube.api.key}") String apiKey) throws GeneralSecurityException, IOException {
        log.info("Initializing YouTube Data API v3 client");
        
        var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        return new YouTube.Builder(httpTransport, jsonFactory, null)
                .setApplicationName("cms-learn-english")
                .setYouTubeRequestInitializer(new YouTubeRequestInitializer(apiKey))
                .build();
    }

    @Bean
    public Cache<String, Object> channelMetadataCache(
            @Value("${youtube.cache.channel-ttl-minutes}") long ttlMinutes,
            @Value("${youtube.cache.max-size}") long maxSize) {
        log.info("Initializing channel metadata cache with TTL: {} minutes, maxSize: {}", ttlMinutes, maxSize);
        
        return CacheBuilder.newBuilder()
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .maximumSize(maxSize)
                .recordStats()
                .build();
    }
}

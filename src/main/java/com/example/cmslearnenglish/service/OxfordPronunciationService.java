package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.VocabularyPronunciationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OxfordPronunciationService {

    private final RestClient restClient;
    private final String baseUrl;
    private final String appId;
    private final String appKey;
    private final Map<String, VocabularyPronunciationResponse> cache = new ConcurrentHashMap<>();

    public OxfordPronunciationService(
            RestClient.Builder restClientBuilder,
            @Value("${oxford.api.base-url:https://od-api-sandbox.oxforddictionaries.com/api/v2}") String baseUrl,
            @Value("${oxford.api.app-id:}") String appId,
            @Value("${oxford.api.app-key:}") String appKey
    ) {
        this.baseUrl = baseUrl;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.appId = appId;
        this.appKey = appKey;
    }

    public VocabularyPronunciationResponse getPronunciation(String word, String accent) {
        if (!StringUtils.hasText(word) || !isConfigured()) {
            return new VocabularyPronunciationResponse(null, null);
        }

        String normalizedWord = word.trim().toLowerCase(Locale.ROOT);
        boolean preferUk = "UK".equalsIgnoreCase(accent);
        String language = preferUk ? "en-gb" : "en-us";
        String cacheKey = language + ':' + normalizedWord;
        VocabularyPronunciationResponse cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        VocabularyPronunciationResponse pronunciation = lookup(normalizedWord, language);
        if (!StringUtils.hasText(pronunciation.audioUrl())) {
            String fallbackLanguage = preferUk ? "en-us" : "en-gb";
            pronunciation = lookup(normalizedWord, fallbackLanguage);
        }
        if (StringUtils.hasText(pronunciation.audioUrl())) {
            cache.put(cacheKey, pronunciation);
        }
        return pronunciation;
    }

    private VocabularyPronunciationResponse lookup(String word, String language) {
        try {
            JsonNode body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/entries/{language}/{word}")
                    .queryParam("fields", "pronunciations")
                    .queryParam("strictMatch", "false")
                    .build(language, word))
                .header(HttpHeaders.ACCEPT, "application/json")
                .header("app_id", appId)
                .header("app_key", appKey)
                .retrieve()
                .body(JsonNode.class);
            VocabularyPronunciationResponse pronunciation = extractPronunciation(body);
            if (!StringUtils.hasText(pronunciation.audioUrl())) {
                log.warn("Oxford pronunciation response has no audioFile for word={} language={} baseUrl={}",
                    word, language, baseUrl);
            }
            return pronunciation;
        } catch (RestClientResponseException exception) {
            log.warn("Oxford pronunciation request failed for word={} language={} baseUrl={} status={} body={}",
                word, language, baseUrl, exception.getStatusCode(), shorten(exception.getResponseBodyAsString()));
            return new VocabularyPronunciationResponse(null, null);
        } catch (RestClientException exception) {
            log.warn("Could not load Oxford pronunciation for word={} language={} baseUrl={}: {}",
                word, language, baseUrl, exception.getMessage());
            return new VocabularyPronunciationResponse(null, null);
        }
    }

    private VocabularyPronunciationResponse extractPronunciation(JsonNode body) {
        if (body == null) {
            return new VocabularyPronunciationResponse(null, null);
        }

        Deque<JsonNode> nodes = new ArrayDeque<>();
        nodes.add(body);
        while (!nodes.isEmpty()) {
            JsonNode node = nodes.removeFirst();
            if (node.isObject()) {
                String audioUrl = text(node, "audioFile");
                if (StringUtils.hasText(audioUrl)) {
                    return new VocabularyPronunciationResponse(text(node, "phoneticSpelling"), audioUrl);
                }
                node.elements().forEachRemaining(nodes::addLast);
            } else if (node.isArray()) {
                node.elements().forEachRemaining(nodes::addLast);
            }
        }
        return new VocabularyPronunciationResponse(null, null);
    }

    private boolean isConfigured() {
        return StringUtils.hasText(appId) && StringUtils.hasText(appKey);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private String shorten(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.length() > 300 ? value.substring(0, 300) + "..." : value;
    }
}

package com.example.cmslearnenglish.controller;

import com.example.cmslearnenglish.dto.AdminVocabularyDtos.*;
import com.example.cmslearnenglish.service.AdminVocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/vocabulary")
@RequiredArgsConstructor
public class AdminVocabularyController {

    private final AdminVocabularyService service;

    @GetMapping("/summary")
    public Summary summary() { return service.getSummary(); }


    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importCsv(@RequestParam("file") MultipartFile file) {
        return service.importCsv(file);
    }

    @GetMapping(value = "/words/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportWordsCsv(
            @RequestParam(required=false) String q,
            @RequestParam(required=false) Long topicId,
            @RequestParam(required=false) Long deckId,
            @RequestParam(defaultValue="id") String sort,
            @RequestParam(defaultValue="DESC") String order) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vocabulary-words.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(service.exportWordsCsv(q, topicId, deckId, sort, order));
    }

    @GetMapping(value = "/words/export-xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportWordsExcel(
            @RequestParam(required=false) String q,
            @RequestParam(required=false) Long topicId,
            @RequestParam(required=false) Long deckId,
            @RequestParam(defaultValue="id") String sort,
            @RequestParam(defaultValue="DESC") String order) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vocabulary-words.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.exportWordsExcel(q, topicId, deckId, sort, order));
    }

    @GetMapping("/decks")
    public Page<DeckResponse> decks(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="25") int size,
            @RequestParam(defaultValue="id") String sort, @RequestParam(defaultValue="DESC") String order,
            @RequestParam(required=false) String q, @RequestParam(required=false) String status) {
        return service.getDecks(page, size, sort, order, q, status);
    }
    @GetMapping("/decks/{id}") public DeckResponse deck(@PathVariable Long id) { return service.getDeck(id); }
    @PostMapping("/decks") public DeckResponse createDeck(@Valid @RequestBody DeckRequest body) { return service.createDeck(body); }
    @PutMapping("/decks/{id}") public DeckResponse updateDeck(@PathVariable Long id, @Valid @RequestBody DeckRequest body) { return service.updateDeck(id, body); }
    @DeleteMapping("/decks/{id}") public ResponseEntity<Void> deleteDeck(@PathVariable Long id) { service.deleteDeck(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/topics")
    public Page<TopicResponse> topics(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="25") int size,
            @RequestParam(defaultValue="id") String sort, @RequestParam(defaultValue="DESC") String order,
            @RequestParam(required=false) String q, @RequestParam(required=false) Long deckId) {
        return service.getTopics(page, size, sort, order, q, deckId);
    }
    @GetMapping("/topics/{id}") public TopicResponse topic(@PathVariable Long id) { return service.getTopic(id); }
    @PostMapping("/topics") public TopicResponse createTopic(@Valid @RequestBody TopicRequest body) { return service.createTopic(body); }
    @PutMapping("/topics/{id}") public TopicResponse updateTopic(@PathVariable Long id, @Valid @RequestBody TopicRequest body) { return service.updateTopic(id, body); }
    @DeleteMapping("/topics/{id}") public ResponseEntity<Void> deleteTopic(@PathVariable Long id) { service.deleteTopic(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/words")
    public Page<WordResponse> words(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="25") int size,
            @RequestParam(defaultValue="id") String sort, @RequestParam(defaultValue="DESC") String order,
            @RequestParam(required=false) String q, @RequestParam(required=false) Long topicId,
            @RequestParam(required=false) Long deckId) {
        return service.getWords(page, size, sort, order, q, topicId, deckId);
    }
    @GetMapping("/words/{id}") public WordResponse word(@PathVariable Long id) { return service.getWord(id); }
    @PostMapping("/words") public WordResponse createWord(@Valid @RequestBody WordRequest body) { return service.createWord(body); }
    @PutMapping("/words/{id}") public WordResponse updateWord(@PathVariable Long id, @Valid @RequestBody WordRequest body) { return service.updateWord(id, body); }
    @DeleteMapping("/words/{id}") public ResponseEntity<Void> deleteWord(@PathVariable Long id) { service.deleteWord(id); return ResponseEntity.noContent().build(); }
}


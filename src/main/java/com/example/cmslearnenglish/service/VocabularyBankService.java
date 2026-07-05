package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.SaveVocabularyRequest;
import com.example.cmslearnenglish.dto.VocabularyBankEntryResponse;
import com.example.cmslearnenglish.entity.User;
import com.example.cmslearnenglish.entity.VocabularyBank;
import com.example.cmslearnenglish.exception.DuplicateVocabularyException;
import com.example.cmslearnenglish.exception.ForbiddenResourceException;
import com.example.cmslearnenglish.exception.ResourceNotFoundException;
import com.example.cmslearnenglish.repository.UserRepository;
import com.example.cmslearnenglish.repository.VocabularyBankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VocabularyBankService {

    private final VocabularyBankRepository vocabularyBankRepository;
    private final UserRepository userRepository;

    public VocabularyBankEntryResponse save(Long userId, String word) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        String normalizedWord = word.toLowerCase().trim();

        if (vocabularyBankRepository.existsByUserIdAndWord(userId, normalizedWord)) {
            throw new DuplicateVocabularyException(normalizedWord);
        }

        VocabularyBank entry = VocabularyBank.builder()
                .user(user)
                .word(normalizedWord)
                .addedAt(Instant.now())
                .build();

        VocabularyBank saved = vocabularyBankRepository.save(entry);
        log.info("Saved word '{}' to vocabulary bank for user={}", normalizedWord, userId);

        return new VocabularyBankEntryResponse(saved.getId(), saved.getWord(), saved.getAddedAt());
    }

    @Transactional(readOnly = true)
    public Page<VocabularyBankEntryResponse> findByUser(Long userId, Pageable pageable) {
        return vocabularyBankRepository.findByUserIdOrderByAddedAtDesc(userId, pageable)
                .map(entry -> new VocabularyBankEntryResponse(entry.getId(), entry.getWord(), entry.getAddedAt()));
    }

    @Transactional(readOnly = true)
    public boolean exists(Long userId, String word) {
        return vocabularyBankRepository.existsByUserIdAndWord(userId, word.toLowerCase().trim());
    }

    public void delete(Long userId, Long entryId) {
        VocabularyBank entry = vocabularyBankRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> {
                    if (vocabularyBankRepository.existsById(entryId)) {
                        throw new ForbiddenResourceException("You do not have permission to delete this vocabulary entry");
                    }
                    throw new ResourceNotFoundException("Vocabulary entry not found with ID: " + entryId);
                });

        vocabularyBankRepository.delete(entry);
        log.info("Deleted vocabulary entry id={} for user={}", entryId, userId);
    }
}

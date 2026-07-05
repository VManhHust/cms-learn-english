package com.example.cmslearnenglish.repository;

import com.example.cmslearnenglish.entity.VocabularyBank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VocabularyBankRepository extends JpaRepository<VocabularyBank, Long> {

    Page<VocabularyBank> findByUserIdOrderByAddedAtDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndWord(Long userId, String word);

    Optional<VocabularyBank> findByIdAndUserId(Long id, Long userId);
}

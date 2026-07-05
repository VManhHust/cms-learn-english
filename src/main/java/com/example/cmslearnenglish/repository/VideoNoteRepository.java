package com.example.cmslearnenglish.repository;

import com.example.cmslearnenglish.entity.VideoNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoNoteRepository extends JpaRepository<VideoNote, Long> {

    Page<VideoNote> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<VideoNote> findByIdAndUserId(Long id, Long userId);

    @Query("""
        SELECT vn FROM VideoNote vn
        JOIN FETCH vn.video
        JOIN FETCH vn.exerciseModuleExtension yme
        WHERE vn.user.id = :userId
        ORDER BY vn.createdAt DESC
        """)
    Page<VideoNote> findByUserIdWithDetails(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT vn FROM VideoNote vn
        JOIN FETCH vn.video
        JOIN FETCH vn.exerciseModuleExtension yme
        JOIN FETCH vn.user
        WHERE vn.user.id = :userId 
        AND vn.exerciseModuleExtension.id = :exerciseModuleExtensionId
        """)
    Optional<VideoNote> findByUserIdAndExerciseModuleExtensionId(
        @Param("userId") Long userId, 
        @Param("exerciseModuleExtensionId") Long exerciseModuleExtensionId
    );
}

package com.example.cmslearnenglish.service;

import com.example.cmslearnenglish.dto.CreateVideoNoteRequest;
import com.example.cmslearnenglish.dto.VideoNoteResponse;
import com.example.cmslearnenglish.entity.*;
import com.example.cmslearnenglish.exception.ResourceNotFoundException;
import com.example.cmslearnenglish.repository.ExerciseModuleRepository;
import com.example.cmslearnenglish.repository.UserRepository;
import com.example.cmslearnenglish.repository.VideoNoteRepository;
import com.example.cmslearnenglish.repository.YoutubeModuleExtensionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service for managing user video notes.
 * Handles creation and retrieval of notes linked to video segments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VideoNoteService {

    private final VideoNoteRepository videoNoteRepository;
    private final YoutubeModuleExtensionRepository moduleExtensionRepository;
    private final UserRepository userRepository;
    private final ExerciseModuleRepository exerciseModuleRepository;

    /**
     * Creates a new video note for a user.
     * If a note already exists for the same user and exercise module, updates the content.
     * 
     * @param userId the ID of the user creating the note
     * @param request the note creation request containing exercise module ID and note content
     * @return the created or updated video note response
     * @throws ResourceNotFoundException if user, exercise module, or video not found
     */
    public VideoNoteResponse createNote(Long userId, CreateVideoNoteRequest request) {
        log.debug("Creating/updating video note for user={}, exerciseModuleId={}", userId, request.exerciseModuleId());
        
        // Check if note already exists
        java.util.Optional<VideoNote> existingNote = videoNoteRepository
            .findByUserIdAndExerciseModuleExtensionId(userId, request.exerciseModuleId());
        
        if (existingNote.isPresent()) {
            // Update existing note
            VideoNote note = existingNote.get();
            note.setNoteContent(request.noteContent().trim());
            VideoNote updated = videoNoteRepository.save(note);
            log.info("Updated video note id={} for user={}", updated.getId(), userId);
            return mapToResponse(updated);
        }
        
        // Create new note
        // Validate user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.warn("User not found: userId={}", userId);
                return new ResourceNotFoundException("User not found");
            });
        
        // Validate exercise module extension exists
        YoutubeModuleExtension moduleExtension = moduleExtensionRepository.findById(request.exerciseModuleId())
            .orElseThrow(() -> {
                log.warn("Exercise module not found: exerciseModuleId={}", request.exerciseModuleId());
                return new ResourceNotFoundException("Exercise module not found");
            });
        
        // Get exercise module to find video
        ExerciseModule exerciseModule = exerciseModuleRepository
            .findByYoutubeModuleExtensionId(request.exerciseModuleId())
            .orElseThrow(() -> {
                log.warn("Exercise module not found for youtubeModuleExtensionId={}", request.exerciseModuleId());
                return new ResourceNotFoundException("Exercise module not found");
            });
        
        LearningTopic video = exerciseModule.getLearningExercise().getLearningTopic();
        
        if (video == null) {
            log.error("Video not found for exerciseModuleId={}", request.exerciseModuleId());
            throw new ResourceNotFoundException("Video not found");
        }
        
        // Create and save note
        VideoNote note = VideoNote.builder()
            .user(user)
            .video(video)
            .exerciseModuleExtension(moduleExtension)
            .noteContent(request.noteContent().trim())
            .createdAt(Instant.now())
            .build();
        
        VideoNote saved = videoNoteRepository.save(note);
        log.info("Created video note id={} for user={}, video={}", saved.getId(), userId, video.getId());
        
        return mapToResponse(saved);
    }

    /**
     * Retrieves all notes for a user with pagination.
     * 
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return page of video note responses
     */
    @Transactional(readOnly = true)
    public Page<VideoNoteResponse> getUserNotes(Long userId, Pageable pageable) {
        log.debug("Fetching video notes for user={}, page={}, size={}", 
            userId, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<VideoNoteResponse> notes = videoNoteRepository.findByUserIdWithDetails(userId, pageable)
            .map(this::mapToResponse);
        
        log.debug("Found {} notes for user={}", notes.getTotalElements(), userId);
        return notes;
    }

    /**
     * Retrieves a note for a specific user and exercise module.
     * 
     * @param userId the ID of the user
     * @param exerciseModuleId the ID of the exercise module
     * @return optional containing the video note response if found
     */
    @Transactional(readOnly = true)
    public java.util.Optional<VideoNoteResponse> getNoteByUserAndModule(Long userId, Long exerciseModuleId) {
        log.debug("Fetching video note for user={}, exerciseModuleId={}", userId, exerciseModuleId);
        
        return videoNoteRepository.findByUserIdAndExerciseModuleExtensionId(userId, exerciseModuleId)
            .map(this::mapToResponse);
    }

    /**
     * Updates an existing video note.
     * 
     * @param userId the ID of the user
     * @param noteId the ID of the note to update
     * @param newContent the new note content
     * @return the updated video note response
     * @throws ResourceNotFoundException if note not found or user doesn't own the note
     */
    public VideoNoteResponse updateNote(Long userId, Long noteId, String newContent) {
        log.debug("Updating video note id={} for user={}", noteId, userId);
        
        VideoNote note = videoNoteRepository.findById(noteId)
            .orElseThrow(() -> {
                log.warn("Video note not found: noteId={}", noteId);
                return new ResourceNotFoundException("Video note not found");
            });
        
        // Verify ownership
        if (!note.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to update note {} owned by user {}", 
                userId, noteId, note.getUser().getId());
            throw new ResourceNotFoundException("Video note not found");
        }
        
        note.setNoteContent(newContent.trim());
        VideoNote updated = videoNoteRepository.save(note);
        log.info("Updated video note id={} for user={}", noteId, userId);
        
        return mapToResponse(updated);
    }

    /**
     * Deletes a video note.
     * 
     * @param userId the ID of the user
     * @param noteId the ID of the note to delete
     * @throws ResourceNotFoundException if note not found or user doesn't own the note
     */
    public void deleteNote(Long userId, Long noteId) {
        log.debug("Deleting video note id={} for user={}", noteId, userId);
        
        VideoNote note = videoNoteRepository.findById(noteId)
            .orElseThrow(() -> {
                log.warn("Video note not found: noteId={}", noteId);
                return new ResourceNotFoundException("Video note not found");
            });
        
        // Verify ownership
        if (!note.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to delete note {} owned by user {}", 
                userId, noteId, note.getUser().getId());
            throw new ResourceNotFoundException("Video note not found");
        }
        
        videoNoteRepository.delete(note);
        log.info("Deleted video note id={} for user={}", noteId, userId);
    }

    /**
     * Maps a VideoNote entity to a VideoNoteResponse DTO.
     * 
     * @param note the video note entity
     * @return the video note response DTO
     */
    private VideoNoteResponse mapToResponse(VideoNote note) {
        // Get the learning exercise title through the relationship chain
        String exerciseTitle = null;
        Long exerciseId = null;
        
        if (note.getExerciseModuleExtension() != null) {
            // Find the ExerciseModule that references this YoutubeModuleExtension
            ExerciseModule exerciseModule = exerciseModuleRepository
                .findByYoutubeModuleExtensionId(note.getExerciseModuleExtension().getId())
                .orElse(null);
            
            if (exerciseModule != null && exerciseModule.getLearningExercise() != null) {
                exerciseTitle = exerciseModule.getLearningExercise().getTitle();
                exerciseId = exerciseModule.getLearningExercise().getId();
            }
        }
        
        // Fallback to video topic name if exercise title not found
        if (exerciseTitle == null && note.getVideo() != null) {
            exerciseTitle = note.getVideo().getTopicName();
            exerciseId = note.getVideo().getId();
        }
        
        return VideoNoteResponse.builder()
            .id(note.getId())
            .videoTitle(exerciseTitle != null ? exerciseTitle : "Unknown")
            .videoId(exerciseId != null ? exerciseId : 0L)
            .englishText(note.getExerciseModuleExtension().getCorrectAnswer())
            .vietnameseText(note.getExerciseModuleExtension().getVietnameseText())
            .noteContent(note.getNoteContent())
            .createdAt(note.getCreatedAt())
            .build();
    }
}

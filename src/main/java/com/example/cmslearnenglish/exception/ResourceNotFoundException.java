package com.example.cmslearnenglish.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Used for user, lesson, or progress not found scenarios.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

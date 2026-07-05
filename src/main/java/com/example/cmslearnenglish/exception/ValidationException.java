package com.example.cmslearnenglish.exception;

/**
 * Exception thrown when data validation fails.
 * Used for custom validation logic beyond standard Bean Validation.
 */
public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

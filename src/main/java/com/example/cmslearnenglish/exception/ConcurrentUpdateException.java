package com.example.cmslearnenglish.exception;

/**
 * Exception thrown when a concurrent update conflict is detected.
 * This occurs when trying to save progress with a stale timestamp.
 */
public class ConcurrentUpdateException extends RuntimeException {
    
    public ConcurrentUpdateException(String message) {
        super(message);
    }
    
    public ConcurrentUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}

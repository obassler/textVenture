package com.osu.textventures.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logger.warn("IllegalArgumentException: {} - Request: {}", ex.getMessage(), request.getDescription(false));
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        logger.error("IllegalStateException: {} - Request: {}", ex.getMessage(), request.getDescription(false));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ExecutionException.class)
    public ResponseEntity<?> handleExecutionException(ExecutionException ex, WebRequest request) {
        logger.error("ExecutionException during Firestore operation: {} - Request: {}",
                ex.getMessage(), request.getDescription(false), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Database operation failed: " + ex.getCause().getMessage()));
    }

    @ExceptionHandler(InterruptedException.class)
    public ResponseEntity<?> handleInterruptedException(InterruptedException ex, WebRequest request) {
        logger.error("InterruptedException during operation: {} - Request: {}",
                ex.getMessage(), request.getDescription(false), ex);
        Thread.currentThread().interrupt();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Operation was interrupted"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex, WebRequest request) {
        logger.error("Unexpected exception: {} - Request: {}",
                ex.getMessage(), request.getDescription(false), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred: " + ex.getMessage()));
    }
}

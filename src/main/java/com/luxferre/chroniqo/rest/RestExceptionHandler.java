package com.luxferre.chroniqo.rest;

import com.luxferre.chroniqo.service.RegistrationDisabledException;
import com.luxferre.chroniqo.service.TimeEntryValidationException;
import com.luxferre.chroniqo.service.user.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Global exception handler for REST controllers.
 * Translates service-layer exceptions into structured JSON error responses.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(TimeEntryValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(TimeEntryValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", ex.getMessage(),
                "field", ex.getField() != null ? ex.getField() : ""
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RegistrationDisabledException.class)
    public ResponseEntity<Map<String, String>> handleRegistrationDisabled(RegistrationDisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }
}

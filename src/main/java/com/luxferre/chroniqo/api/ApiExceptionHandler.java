package com.luxferre.chroniqo.api;

import com.luxferre.chroniqo.service.RegistrationDisabledException;
import com.luxferre.chroniqo.service.TimeEntryValidationException;
import com.luxferre.chroniqo.service.user.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates exceptions thrown by the REST controllers and services into
 * RFC 9457 {@link ProblemDetail} responses.
 *
 * <p>Validation problems carry a machine-readable {@code code} and, where
 * applicable, the affected {@code field} so that the frontend can show the
 * message next to the right input.
 *
 * @author Luxferre86
 * @since 14.11.2026
 */
@SuppressWarnings("unused")
@Slf4j
@RestControllerAdvice(basePackages = "com.luxferre.chroniqo.api")
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Bean Validation failures on request bodies: {@code 400} with an
     * {@code errors} map of field name to message.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Business-rule violations of time entries and absences.
     */
    @ExceptionHandler(TimeEntryValidationException.class)
    public ResponseEntity<ProblemDetail> handleTimeEntryValidation(TimeEntryValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setProperty("code", ex.getErrorType().name());
        problem.setProperty("field", ex.getField());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problem);
    }

    /**
     * Registration is disabled by configuration.
     */
    @ExceptionHandler(RegistrationDisabledException.class)
    public ResponseEntity<ProblemDetail> handleRegistrationDisabled(RegistrationDisabledException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setProperty("code", "REGISTRATION_DISABLED");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    /**
     * The session does not belong to an existing user (e.g. account deleted).
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Not authenticated");
        problem.setProperty("code", "UNAUTHENTICATED");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    /**
     * Rejected input detected by the services (duplicate email, wrong current
     * password, unknown country, ...). Reported as {@code 400}.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setProperty("code", "INVALID_REQUEST");
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Anything unexpected: logged, but reported without internal details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unexpected error while handling API request", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again.");
        problem.setProperty("code", "INTERNAL_ERROR");
        return ResponseEntity.internalServerError().body(problem);
    }
}

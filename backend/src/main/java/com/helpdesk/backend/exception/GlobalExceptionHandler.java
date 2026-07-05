package com.helpdesk.backend.exception;

import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.helpdesk.backend.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles a duplicate email registration attempt.
     *
     * @param ex the thrown exception
     * @return HTTP 409 Conflict with the error message
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        // Conflict: the email is already taken
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Handles lookups for entities that do not exist.
     *
     * @param ex the thrown exception
     * @return HTTP 404 Not Found with the error message
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        // Not found: the requested resource is missing
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Handles illegal ticket status transitions.
     *
     * @param ex the thrown exception
     * @return HTTP 400 Bad Request with the error message
     */
    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidTransitionException ex) {
        // Bad request: the attempted status transition is not allowed
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Handles attempts to assign a ticket to a user who lacks the required role.
     *
     * @param ex the thrown exception
     * @return HTTP 400 Bad Request with the error message
     */
    @ExceptionHandler(InvalidAssigneeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAssignee(InvalidAssigneeException ex) {
        // Bad request: only AGENTs and ADMINs may be assigned a ticket
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * Handles failed authentication attempts.
     *
     * @param ex the thrown exception
     * @return HTTP 401 Unauthorized with a generic message
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        // Unauthorized: return a generic message to avoid leaking which field was wrong
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid credentials"));
    }

    /**
     * Handles attempts to access a forbidden resource or action.
     *
     * @param ex the thrown exception
     * @return HTTP 403 Forbidden with a generic message
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        // Forbidden: the user is authenticated but not allowed to perform this action
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Access denied"));
    }

    /**
     * Handles bean validation failures triggered by {@code @Valid} on request bodies.
     *
     * @param ex the thrown exception containing field-level error details
     * @return HTTP 400 Bad Request listing which fields failed and why
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // Collect each field error into a single readable message (field: reason, ...)
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }

    /**
     * Handles requests with a malformed or unreadable JSON body.
     *
     * @param ex the thrown exception
     * @return HTTP 400 Bad Request with a generic message — no internal parse details exposed
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        // Return a generic message to avoid leaking parser internals or class names
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Malformed or unreadable request body"));
    }

    /**
     * Handles database constraint violations (unique keys, foreign keys, not-null).
     *
     * @param ex the thrown exception
     * @return HTTP 409 Conflict with a generic message — no schema details exposed
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Return a generic conflict message; never surface column/table names from the cause
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Data integrity violation"));
    }

    /**
     * Catch-all handler that prevents unhandled exceptions from reaching Spring Boot's
     * default /error endpoint, which could expose stack traces or internal details.
     *
     * @param ex the unhandled exception
     * @return HTTP 500 Internal Server Error with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        // Never surface internal details to the client; exceptions should be logged by the framework
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("An unexpected error occurred"));
    }

}
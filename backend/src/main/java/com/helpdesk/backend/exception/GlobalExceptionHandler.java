package com.helpdesk.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
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
     * @return HTTP 403 Forbidden with the error message
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException ex){
        // Forbidden: the user is authenticated but not allowed to perform this action
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

}
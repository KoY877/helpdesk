package com.helpdesk.backend.exception;

public class InvalidAssigneeException extends RuntimeException {
    public InvalidAssigneeException(String message) {
        super(message);
    }
}

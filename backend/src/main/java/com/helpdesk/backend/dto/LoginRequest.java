package com.helpdesk.backend.dto;

public record LoginRequest (
    String email,
    String password
) {
    
}

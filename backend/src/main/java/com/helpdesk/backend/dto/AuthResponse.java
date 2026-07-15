package com.helpdesk.backend.dto;

public record AuthResponse(
    String token,
    String refreshToken,
    String role, 
    String userId
) {} 

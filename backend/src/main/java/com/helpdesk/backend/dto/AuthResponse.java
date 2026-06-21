package com.helpdesk.backend.dto;

import java.util.UUID;

public record AuthResponse(
    String token,
    String role, 
    UUID userId
) {} 

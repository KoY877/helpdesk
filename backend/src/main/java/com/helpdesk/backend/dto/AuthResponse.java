package com.helpdesk.backend.dto;

import io.micrometer.common.lang.NonNull;

public record AuthResponse(
    String token,
    String refreshToken,
    String role, 
    String userId
) {} 

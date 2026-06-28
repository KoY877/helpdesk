package com.helpdesk.backend.dto;

import jakarta.validation.constraints.NotNull;

public record RefreshTokenRequest (
    @NotNull String refreshToken
) {
    
}

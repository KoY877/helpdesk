package com.helpdesk.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.helpdesk.backend.model.enums.Role;

public record UserResponse(
    UUID id,
    String name,
    String email,
    Role role,
    int order,
    LocalDateTime createdAt
) {}
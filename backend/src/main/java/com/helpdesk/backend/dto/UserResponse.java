package com.helpdesk.backend.dto;

import java.time.LocalDateTime;

import com.helpdesk.backend.model.enums.Role;

public record UserResponse(
    String id,
    String name,
    String email,
    Role role,
    int order,
    LocalDateTime createdAt
) {}
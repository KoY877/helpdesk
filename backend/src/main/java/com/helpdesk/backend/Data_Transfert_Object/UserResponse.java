package com.helpdesk.backend.Data_Transfert_Object;

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
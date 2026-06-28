package com.helpdesk.backend.dto;

import com.helpdesk.backend.model.enums.Role;

import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(
    @NotNull Role role
) {

}

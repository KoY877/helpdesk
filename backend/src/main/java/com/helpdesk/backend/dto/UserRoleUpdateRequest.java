package com.helpdesk.backend.dto;

import com.helpdesk.backend.model.enums.Role;

public record UserRoleUpdateRequest(Role role) {
    
}

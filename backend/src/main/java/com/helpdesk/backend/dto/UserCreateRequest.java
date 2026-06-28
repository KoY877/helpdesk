package com.helpdesk.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
    String name,
    @Email String email,
    @Size(min = 12) String password
) {}

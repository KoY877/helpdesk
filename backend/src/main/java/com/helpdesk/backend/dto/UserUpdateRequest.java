package com.helpdesk.backend.dto;

public record UserUpdateRequest(
    String name,
    String email,
    String password
) {}

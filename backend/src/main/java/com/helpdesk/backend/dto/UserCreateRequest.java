package com.helpdesk.backend.dto;


public record UserCreateRequest(
    String name,
    String email,
    String password
) {}

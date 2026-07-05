package com.helpdesk.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record TicketUpdateRequest(
    @NotBlank String title,
    @NotBlank String description
) {}
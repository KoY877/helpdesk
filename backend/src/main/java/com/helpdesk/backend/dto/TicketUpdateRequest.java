package com.helpdesk.backend.dto;

import jakarta.validation.constraints.NotNull;

public record TicketUpdateRequest(
    @NotNull String title,
    @NotNull String description
) {}
package com.helpdesk.backend.dto;

import jakarta.validation.constraints.NotNull;

public record TicketCreateRequest (
    @NotNull String title, 
    @NotNull String description) {
    
}

package com.helpdesk.backend.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record TicketAssignRequest(
    @NotNull UUID assignedToId
) {}

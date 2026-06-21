package com.helpdesk.backend.dto;

import com.helpdesk.backend.model.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record TicketStatusUpdateRequest(@NotNull TicketStatus status) {}

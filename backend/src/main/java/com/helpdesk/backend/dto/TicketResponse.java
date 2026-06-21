package com.helpdesk.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.helpdesk.backend.model.enums.TicketStatus;

public record TicketResponse(
    UUID id,
    int order,
    String title,
    String description,
    TicketStatus status,
    UUID createdById,
    String createdByName,
    UUID assignedToId,
    String assignedToName,
    LocalDateTime createdAt
) {}
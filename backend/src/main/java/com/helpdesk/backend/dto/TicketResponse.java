package com.helpdesk.backend.dto;

import java.time.LocalDateTime;

import com.helpdesk.backend.model.enums.TicketStatus;

public record TicketResponse(
    String id,
    int order,
    String title,
    String description,
    TicketStatus status,
    String createdById,
    String createdByName,
    String assignedToId,
    String assignedToName,
    LocalDateTime createdAt
) {}
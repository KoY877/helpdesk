package com.helpdesk.backend.Data_Transfert_Object;

import java.time.LocalDateTime;

import com.helpdesk.backend.model.enums.Ticketstatus;

public record TicketResponse(
    String id,
    int order,
    String title,
    String description,
    Ticketstatus status,
    String createdById,
    String createdByName,
    String assignedToId,
    String assignedToName,
    LocalDateTime createdAt
) {}
package com.helpdesk.backend.dto;


public record TicketUpdateRequest(
    String title,
    String description
) {}
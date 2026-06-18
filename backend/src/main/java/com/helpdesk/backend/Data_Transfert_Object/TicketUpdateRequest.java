package com.helpdesk.backend.Data_Transfert_Object;


public record TicketUpdateRequest(
    String title,
    String description
) {}
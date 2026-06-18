package com.helpdesk.backend.Data_Transfert_Object;

import jakarta.validation.constraints.NotNull;

public record TicketAssignRequest(
    @NotNull String assignedToId
) {}

package com.helpdesk.backend.Data_Transfert_Object;

import com.helpdesk.backend.model.enums.Ticketstatus;
import jakarta.validation.constraints.NotNull;

public record TicketStatusUpdateRequest(@NotNull Ticketstatus status) {}

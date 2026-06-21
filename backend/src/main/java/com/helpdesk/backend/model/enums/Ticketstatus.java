package com.helpdesk.backend.model.enums;

import java.util.Map;
import java.util.Set;

public enum TicketStatus {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED;

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = Map.of(
        OPEN,        Set.of(IN_PROGRESS),
        IN_PROGRESS, Set.of(RESOLVED),
        RESOLVED,    Set.of(CLOSED, IN_PROGRESS),
        CLOSED,      Set.of()
    );

    public boolean canTransitionTo(TicketStatus next) {
        return ALLOWED.get(this).contains(next);
    }
}

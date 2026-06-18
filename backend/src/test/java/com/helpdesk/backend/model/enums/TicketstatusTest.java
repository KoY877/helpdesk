package com.helpdesk.backend.model.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TicketstatusTest {

    @Test
    void open_canTransitionTo_inProgress() {
        assertTrue(Ticketstatus.OPEN.canTransitionTo(Ticketstatus.IN_PROGRESS));
    }

    @Test
    void inProgress_canTransitionTo_resolved() {
        assertTrue(Ticketstatus.IN_PROGRESS.canTransitionTo(Ticketstatus.RESOLVED));
    }

    @Test
    void resolved_canTransitionTo_closed() {
        assertTrue(Ticketstatus.RESOLVED.canTransitionTo(Ticketstatus.CLOSED));
    }

    @Test
    void closed_cannotTransitionTo_anything() {
        assertFalse(Ticketstatus.CLOSED.canTransitionTo(Ticketstatus.OPEN));
        assertFalse(Ticketstatus.CLOSED.canTransitionTo(Ticketstatus.IN_PROGRESS));
        assertFalse(Ticketstatus.CLOSED.canTransitionTo(Ticketstatus.RESOLVED));
        assertFalse(Ticketstatus.CLOSED.canTransitionTo(Ticketstatus.CLOSED));
    }
}

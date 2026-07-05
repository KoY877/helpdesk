package com.helpdesk.backend.model.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TicketstatusTest {

    @Test
    void open_canTransitionTo_inProgress() {
        assertTrue(TicketStatus.OPEN.canTransitionTo(TicketStatus.IN_PROGRESS));
    }

    @Test
    void inProgress_canTransitionTo_resolved() {
        assertTrue(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.RESOLVED));
    }

    @Test
    void resolved_canTransitionTo_closed() {
        assertTrue(TicketStatus.RESOLVED.canTransitionTo(TicketStatus.CLOSED));
    }

    @Test
    void closed_canTransitionTo_inProgress() {
        assertTrue(TicketStatus.CLOSED.canTransitionTo(TicketStatus.IN_PROGRESS));
    }

    @Test
    void closed_cannotTransitionTo_others() {
        assertFalse(TicketStatus.CLOSED.canTransitionTo(TicketStatus.OPEN));
        assertFalse(TicketStatus.CLOSED.canTransitionTo(TicketStatus.RESOLVED));
        assertFalse(TicketStatus.CLOSED.canTransitionTo(TicketStatus.CLOSED));
    }
}

package com.helpdesk.backend.dto;

import com.helpdesk.backend.model.Ticket;

public class TicketMapper {
    /**
     * Maps a {@link Ticket} entity to its {@link TicketResponse} DTO,
     * flattening the creator and (optional) assignee into id/name pairs.
     *
     * @param ticket the entity to map
     * @return the corresponding response DTO
     */
    public static TicketResponse toResponse(Ticket ticket) {
        // Flatten the related users; the assignee may be absent, so guard against null
        return new TicketResponse(
            ticket.getId(),
            ticket.getOrder(),
            ticket.getTitle(),
            ticket.getDescription(),
            ticket.getStatus(),
            ticket.getCreatedBy().getId(),
            ticket.getCreatedBy().getName(),
            // Assignee is optional: expose null when the ticket is unassigned
            ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null,
            ticket.getAssignedTo() != null ? ticket.getAssignedTo().getName() : null,
            ticket.getCreatedAt()
        );
    }
}

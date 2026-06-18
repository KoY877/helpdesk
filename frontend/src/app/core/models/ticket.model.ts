/** A ticket as returned by the backend. */
export interface Ticket {
  id: string;
  order: number;
  title: string;
  description: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
  createdById: string;
  createdByName: string;
  assignedToId: string | null;
  assignedToName: string | null;
  createdAt: string;
}

/** Payload for creating a ticket. */
export interface TicketCreateRequest {
  title: string;
  description: string;
}

/** Payload for changing a ticket's status. */
export interface TicketStatusRequest {
  status: string;
}

/** Payload for assigning a ticket to a user. */
export interface TicketAssignRequest {
  assignedToId: string;
}

/** Extended status-change payload carrying the ticket id and requester email. */
export interface TicketStatusRequest {
  id: string ,
  email: string,
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
}

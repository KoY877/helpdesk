import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Ticket, TicketAssignRequest, TicketCreateRequest, TicketStatusRequest } from '../models/ticket.model';
import { Observable } from 'rxjs';
import { environement } from '../environements/environements';

/**
 * Client-side gateway to the ticket-related REST endpoints.
 */
@Injectable({
  providedIn: 'root',
})
export class TicketService {
  // Base API URL taken from the active environment
  private readonly apiUrl = environement.apiUrl;

  constructor(
    private readonly http: HttpClient
  ){}

  /**
   * Creates a new ticket.
   * @param request the ticket data (title, description)
   * @returns an Observable emitting the created ticket
   */
  createTicket(request: TicketCreateRequest): Observable<Ticket>{
    return this.http.post<Ticket> (`${this.apiUrl}/tickets`, request);
  }

  /**
   * Fetches every ticket in the system.
   * @returns an Observable emitting the list of all tickets
   */
  getAllTickets(): Observable<Ticket[]>{
    return this.http.get<Ticket[]> (`${this.apiUrl}/tickets/all`);
  }

  /**
   * Fetches the tickets created by a given user.
   * @param userId the author's unique identifier
   * @returns an Observable emitting the user's tickets
   */
  getTicketsByUserId(userId: string): Observable<Ticket[]>{
    return this.http.get<Ticket[]> (`${this.apiUrl}/tickets/user/${userId}`);
  }

  /**
   * Fetches the tickets visible to the authenticated user.
   * Visibility is resolved server-side from the JWT (USER sees their own,
   * AGENT/ADMIN see all).
   * @returns an Observable emitting the visible tickets
   */
  getVisibleTickets(): Observable<Ticket[]>{
    return this.http.get<Ticket[]> (`${this.apiUrl}/tickets`);
  }

  /**
   * Updates a ticket's title and/or description.
   * @param request the fields to update
   * @param id the ticket's unique identifier
   * @returns an Observable emitting the updated ticket
   */
  updateTicket(request: TicketCreateRequest, id: string): Observable<Ticket>{
    return this.http.patch<Ticket> (`${this.apiUrl}/tickets/${id}`, request);
  }

  /**
   * Changes a ticket's status through a validated transition.
   * @param request the target status
   * @param id the ticket's unique identifier
   * @returns an Observable emitting the updated ticket
   */
  updateTicketStatus(request: TicketStatusRequest, id: string): Observable<Ticket>{
    return this.http.patch<Ticket> (`${this.apiUrl}/tickets/${id}/status`, request);
  }

  /**
   * Assigns a ticket to a user (admin-only endpoint).
   * @param id the ticket's unique identifier
   * @param request the assignee data
   * @returns an Observable emitting the updated ticket
   */
  assignTicket(id: string, request: TicketAssignRequest): Observable<Ticket>{
    return this.http.patch<Ticket> (`${this.apiUrl}/tickets/${id}/assign`, request);
  }

  /**
   * Deletes a ticket by id.
   * @param id the ticket's unique identifier
   * @returns an Observable completing when the ticket is deleted
   */
  deleteTicket( id: string): Observable<void>{
    return this.http.delete<void> (`${this.apiUrl}/tickets/${id}`);
  }
}

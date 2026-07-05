package com.helpdesk.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helpdesk.backend.dto.TicketAssignRequest;
import com.helpdesk.backend.dto.TicketCreateRequest;
import com.helpdesk.backend.dto.TicketResponse;
import com.helpdesk.backend.dto.TicketStatusUpdateRequest;
import com.helpdesk.backend.dto.TicketUpdateRequest;
import com.helpdesk.backend.service.TicketService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tickets")
@Validated
public class TicketController {

    private final TicketService ticketService;

    /**
     * Returns every ticket in the system.
     *
     * @return HTTP 200 with the list of all tickets
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<List<TicketResponse>> getAllTickets() {
        // Delegate to the service and return the full list
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    /**
     * Returns the tickets visible to the authenticated user.
     *
     * @param userDetails the currently authenticated user
     * @return HTTP 200 with the list of visible tickets
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<List<TicketResponse>> getVisibleTickets(
            @AuthenticationPrincipal UserDetails userDetails) {
        // Use the authenticated email to scope the visible tickets
        return ResponseEntity.ok(ticketService.getVisibleTickets(userDetails.getUsername()));
    }

    /**
     * Returns a single ticket by id.
     * A USER may only retrieve a ticket they created; AGENTs and ADMINs see any.
     *
     * @param id          the unique identifier of the ticket
     * @param userDetails the currently authenticated user
     * @return HTTP 200 with the matching ticket
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<TicketResponse> getTicketById(
            @PathVariable @NotNull String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Pass the caller's identity so the service can enforce ownership
        return ResponseEntity.ok(ticketService.getTicketById(id, userDetails.getUsername()));
    }

    /**
     * Returns the tickets created by a given user.
     * A USER may only query their own id; AGENTs and ADMINs may query any user.
     *
     * @param userId      the unique identifier of the author
     * @param userDetails the currently authenticated user
     * @return HTTP 200 with the list of tickets created by the user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<List<TicketResponse>> getTicketsByUserId(
            @PathVariable @NotNull String userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Pass the caller's identity so the service can enforce ownership
        return ResponseEntity.ok(ticketService.getTicketsByUserId(userId, userDetails.getUsername()));
    }

    /**
     * Creates a new ticket for the authenticated user.
     *
     * @param request     the ticket data (title, description)
     * @param userDetails the currently authenticated user (the author)
     * @return HTTP 201 with the created ticket
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<TicketResponse> createTicket(
            @RequestBody @Valid TicketCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Pass the author's email so the service can attach the creator
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.createTicket(request, userDetails.getUsername()));
    }

    /**
     * Updates a ticket's title and description.
     * A USER may only update a ticket they created; AGENTs and ADMINs may update any.
     *
     * @param id          the unique identifier of the ticket
     * @param request     the new title and description
     * @param userDetails the currently authenticated user
     * @return HTTP 200 with the updated ticket
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable @NotNull String id,
            @RequestBody @Valid TicketUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Pass the caller's identity so the service can enforce ownership
        return ResponseEntity.ok(ticketService.updateTicket(id, request, userDetails.getUsername()));
    }

    /**
     * Changes the status of a ticket through a validated transition.
     *
     * @param id          the unique identifier of the ticket
     * @param request     the target status
     * @param userDetails the currently authenticated user
     * @return HTTP 200 with the updated ticket
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable @NotNull String id,
            @RequestBody @Valid TicketStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
        ) {
        // The service validates both permissions and the transition itself
        return ResponseEntity.ok(ticketService.transition(id, request.status(), userDetails.getUsername()));
    }

    /**
     * Assigns a ticket to a user. Restricted to administrators.
     *
     * @param id      the unique identifier of the ticket
     * @param request the assignee data
     * @return HTTP 200 with the updated ticket
     */
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable @NotNull String id,
            @RequestBody @Valid TicketAssignRequest request) {
        // Delegate the assignment to the service
        return ResponseEntity.ok(ticketService.assignTicket(id, request.assignedToId()));
    }

    /**
     * Deletes a ticket by id. Restricted to administrators.
     *
     * @param id the unique identifier of the ticket
     * @return HTTP 204 with no content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTicket(@PathVariable @NotNull String id) {
        // Delete the ticket then return an empty 204 response
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}
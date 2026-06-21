package com.helpdesk.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.helpdesk.backend.dto.TicketCreateRequest;
import com.helpdesk.backend.dto.TicketMapper;
import com.helpdesk.backend.dto.TicketResponse;
import com.helpdesk.backend.dto.TicketUpdateRequest;
import com.helpdesk.backend.exception.InvalidTransitionException;
import com.helpdesk.backend.exception.ResourceNotFoundException;
import com.helpdesk.backend.model.Ticket;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.model.enums.Role;
import com.helpdesk.backend.model.enums.TicketStatus;
import com.helpdesk.backend.repository.TicketRepository;
import com.helpdesk.backend.repository.UserRepository;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    /**
     * Retrieves every ticket in the system.
     *
     * @return the list of all tickets mapped to {@link TicketResponse}
     */
    @Transactional
    public List<TicketResponse> getAllTickets() {
        // Load all tickets and map each one to its response DTO
        return ticketRepository.findAll().stream().map(TicketMapper::toResponse).toList();
    }

    /**
     * Retrieves a single ticket by id.
     *
     * @param id the unique identifier of the ticket
     * @return the matching ticket as a {@link TicketResponse}
     * @throws ResourceNotFoundException if no ticket matches the id
     */
    @Transactional
    public TicketResponse getTicketById(@NotNull UUID id) {
        // Find the ticket, map it, or throw if it is missing
        return ticketRepository.findById(id).map(TicketMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));
    }

    /**
     * Retrieves the tickets created by a given user.
     *
     * @param userId the unique identifier of the author
     * @return the list of tickets created by the user
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional
    public List<TicketResponse> getTicketsByUserId(@NotNull UUID userId) {
        // Make sure the user exists before querying their tickets
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        // Fetch the tickets authored by the user and map them
        return ticketRepository.findByCreatedBy_Id(userId).stream()
                .map(TicketMapper::toResponse).toList();
    }

    /**
     * Returns the tickets visible to the authenticated user:
     * a USER only sees the tickets they created, while AGENT and ADMIN see all.
     *
     * @param email the email of the authenticated user
     * @return the list of tickets the user is allowed to see
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional
    public List<TicketResponse> getVisibleTickets(@NotNull String email) {
        // Resolve the authenticated user from their email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // A plain USER only sees their own tickets; AGENT/ADMIN see everything
        return user.getRole() == Role.USER
                ? getTicketsByUserId(user.getId())
                : getAllTickets();
    }

    /**
     * Creates a new ticket on behalf of the given author.
     *
     * @param request     the ticket data (title, description)
     * @param authorEmail the email of the user creating the ticket
     * @return the created ticket as a {@link TicketResponse}
     * @throws ResourceNotFoundException if the author does not exist
     */
    @Transactional
    public TicketResponse createTicket(@NotNull TicketCreateRequest request, String authorEmail) {
        // Resolve the author from their email
        User creator = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Build the ticket; a new ticket always starts in OPEN status
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(creator);
        // Place the ticket at the end of the ordering sequence
        ticket.setOrder(ticketRepository.findMaxOrder() + 1);

        // Log only the id to avoid leaking PII, then persist and return
        log.info("Ticket created by user id: {}", creator.getId());
        return TicketMapper.toResponse(ticketRepository.save(ticket));
    }

    /**
     * Partially updates a ticket's title and/or description.
     * The status is never modified here; use {@link #transition} instead.
     *
     * @param id      the unique identifier of the ticket
     * @param request the fields to update
     * @return the updated ticket as a {@link TicketResponse}
     * @throws ResourceNotFoundException if no ticket matches the id
     */
    @Transactional
    public TicketResponse updateTicket(@NotNull UUID id, TicketUpdateRequest request) {
        // Fetch the ticket or throw if it is missing
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));

        // Only overwrite the fields that were provided
        if (request.title() != null) ticket.setTitle(request.title());
        if (request.description() != null) ticket.setDescription(request.description());

        // Persist and return the updated ticket
        return TicketMapper.toResponse(ticketRepository.save(ticket));
    }

    /**
     * Assigns a ticket to an agent and moves it to IN_PROGRESS.
     *
     * @param ticketId     the unique identifier of the ticket
     * @param assignedToId the unique identifier of the assignee
     * @return the updated ticket as a {@link TicketResponse}
     * @throws ResourceNotFoundException  if the ticket or the assignee does not exist
     * @throws InvalidTransitionException if the ticket cannot move to IN_PROGRESS
     */
    @Transactional
    public TicketResponse assignTicket(@NotNull UUID ticketId, @NotNull UUID assignedToId) {
        // Fetch the ticket or throw if it is missing
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        // Assigning implies moving to IN_PROGRESS, so validate that transition first
        if (!ticket.getStatus().canTransitionTo(TicketStatus.IN_PROGRESS)) {
            throw new InvalidTransitionException(
                "Cannot assign ticket in status " + ticket.getStatus());
        }

        // Resolve the assignee or throw if it is missing
        User assignedTo = userRepository.findById(assignedToId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + assignedToId));

        // Apply the assignment and advance the status
        ticket.setAssignedTo(assignedTo);
        ticket.setStatus(TicketStatus.IN_PROGRESS);

        // Log only ids, then persist and return
        log.info("Ticket id: {} assigned to user id: {}", ticketId, assignedToId);
        return TicketMapper.toResponse(ticketRepository.save(ticket));
    }

    /**
     * Deletes a ticket by id.
     *
     * @param id the unique identifier of the ticket
     * @throws ResourceNotFoundException if no ticket matches the id
     */
    @Transactional
    public void deleteTicket(@NotNull UUID id) {
        // Make sure the ticket exists before attempting to delete
        if (!ticketRepository.existsById(id)) {
            throw new ResourceNotFoundException("Ticket not found: " + id);
        }

        // Remove the ticket from the database
        ticketRepository.deleteById(id);
    }

    /**
     * Transitions a ticket to a target status after checking permissions and
     * the validity of the transition.
     * <p>
     * A USER may only mark their own IN_PROGRESS ticket as RESOLVED, while an
     * AGENT or ADMIN may perform any valid transition.
     *
     * @param id           the unique identifier of the ticket
     * @param targetStatus the status to move the ticket to
     * @param userEmail    the email of the user requesting the transition
     * @return the updated ticket as a {@link TicketResponse}
     * @throws ResourceNotFoundException  if the ticket or the user does not exist
     * @throws AccessDeniedException      if the user is not allowed to perform the transition
     * @throws InvalidTransitionException if the transition is not allowed by the state machine
     */
    @Transactional
    public TicketResponse transition(@NotNull UUID id, TicketStatus targetStatus, String userEmail) {
        // Fetch the ticket or throw if it is missing
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));

        // Resolve the requesting user from their email
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new ResourceNotFoundException("User not found "));

        // Determine whether the user owns the ticket
        boolean isCreator = ticket.getCreatedBy().getId().equals(user.getId());

        // A USER may only resolve their own ticket while it is IN_PROGRESS
        boolean isUserAllowed = user.getRole() == Role.USER
                && isCreator
                && ticket.getStatus() == TicketStatus.IN_PROGRESS
                && targetStatus == TicketStatus.RESOLVED;

        // Agents and admins may perform any valid transition
        boolean isAgentOrAdmin = user.getRole() == Role.AGENT || user.getRole() == Role.ADMIN;

        // Deny the request if neither rule grants permission
        if (!isUserAllowed && !isAgentOrAdmin) {
            throw new AccessDeniedException("Not allowed to perform this transition");
        }

        // Validate the transition against the status state machine
        if (!ticket.getStatus().canTransitionTo(targetStatus)) {
            throw new InvalidTransitionException(
                    "Cannot transition from " + ticket.getStatus() + " to " + targetStatus);
        }

        // Apply the new status, log only ids, then persist and return
        ticket.setStatus(targetStatus);
        log.info("Ticket id: {} transitioned to {}", id, targetStatus);
        return TicketMapper.toResponse(ticketRepository.save(ticket));
    }
}
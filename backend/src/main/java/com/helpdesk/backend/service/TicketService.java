package com.helpdesk.backend.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.helpdesk.backend.dto.TicketCreateRequest;
import com.helpdesk.backend.dto.TicketMapper;
import com.helpdesk.backend.dto.TicketResponse;
import com.helpdesk.backend.dto.TicketUpdateRequest;
import com.helpdesk.backend.exception.InvalidAssigneeException;
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
     * Retrieves a single ticket by id, enforcing visibility: a USER may only
     * read a ticket they created; AGENTs and ADMINs may read any ticket.
     *
     * @param id          the unique identifier of the ticket
     * @param callerEmail the email of the authenticated caller
     * @return the matching ticket as a {@link TicketResponse}
     * @throws ResourceNotFoundException if no ticket matches the id
     * @throws AccessDeniedException     if a USER tries to read a ticket they do not own
     */
    @Transactional
    public TicketResponse getTicketById(@NotNull String id, @NotNull String callerEmail) {
        // Find the ticket or throw if missing
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));

        // Resolve the caller so we can check their role and ownership
        User caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // A plain USER may only view tickets they created
        if (caller.getRole() == Role.USER && !ticket.getCreatedBy().getId().equals(caller.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        return TicketMapper.toResponse(ticket);
    }

    /**
     * Retrieves the tickets created by a given user, enforcing visibility: a USER
     * may only query their own id; AGENTs and ADMINs may query any user.
     *
     * @param userId      the unique identifier of the author
     * @param callerEmail the email of the authenticated caller
     * @return the list of tickets created by the user
     * @throws ResourceNotFoundException if the target user does not exist
     * @throws AccessDeniedException     if a USER tries to list another user's tickets
     */
    @Transactional
    public List<TicketResponse> getTicketsByUserId(@NotNull String userId, @NotNull String callerEmail) {
        // Resolve the caller to check their role
        User caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // A plain USER may only list their own tickets
        if (caller.getRole() == Role.USER && !caller.getId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        // For AGENT/ADMIN querying a different user, verify the target exists
        if (!caller.getId().equals(userId) && !userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        // Fetch the tickets authored by the target user and map them
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
                ? getTicketsByUserId(user.getId(), email)
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
     * Updates a ticket's title and description.
     * The status is never modified here; use {@link #transition} instead.
     * A USER may only update a ticket they created; AGENTs and ADMINs may update any.
     *
     * @param id          the unique identifier of the ticket
     * @param request     the new title and description
     * @param callerEmail the email of the authenticated caller
     * @return the updated ticket as a {@link TicketResponse}
     * @throws ResourceNotFoundException if no ticket matches the id
     * @throws AccessDeniedException     if a USER tries to update a ticket they do not own
     */
    @Transactional
    public TicketResponse updateTicket(@NotNull String id, TicketUpdateRequest request, @NotNull String callerEmail) {
        // Fetch the ticket or throw if it is missing
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));

        // Resolve the caller to check their role and ownership
        User caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // A plain USER may only edit their own ticket
        if (caller.getRole() == Role.USER && !ticket.getCreatedBy().getId().equals(caller.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        // Both fields are required and validated non-blank by TicketUpdateRequest
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());

        // Persist and return the updated ticket
        return TicketMapper.toResponse(ticketRepository.save(ticket));
    }

    /**
     * Assigns a ticket to an agent and moves it to IN_PROGRESS.
     * The assignee must hold the AGENT or ADMIN role; assigning to a plain USER is rejected.
     *
     * @param ticketId     the unique identifier of the ticket
     * @param assignedToId the unique identifier of the assignee
     * @return the updated ticket as a {@link TicketResponse}
     * @throws ResourceNotFoundException  if the ticket or the assignee does not exist
     * @throws InvalidTransitionException if the ticket cannot move to IN_PROGRESS
     * @throws InvalidAssigneeException   if the assignee does not have the AGENT or ADMIN role
     */
    @Transactional
    public TicketResponse assignTicket(@NotNull String ticketId, @NotNull String assignedToId) {
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

        // Only AGENTs and ADMINs may handle tickets; reject plain USERs to prevent inconsistent state
        if (assignedTo.getRole() == Role.USER) {
            throw new InvalidAssigneeException(
                "User id: " + assignedToId + " does not have the required role to be assigned a ticket");
        }

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
    public void deleteTicket(@NotNull String id) {
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
    public TicketResponse transition(@NotNull String id, TicketStatus targetStatus, String userEmail) {
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
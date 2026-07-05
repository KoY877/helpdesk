package com.helpdesk.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.helpdesk.backend.dto.TicketCreateRequest;
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

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    // Mocks of the dependencies injected into TicketService
    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private TicketService ticketService;

    /**
     * Verifies that createTicket returns a valid TicketResponse
     * when the user exists in the database.
     */
    @Test
    void createTicket_withValidUser_returnsTicketResponse() {
        // Arrange: existing user
        String userId = "u1";
        String ticketId = "t1";

        User user = new User();
        user.setId(userId);
        user.setEmail("test@test.com");

        // Arrange: ticket returned after saving
        Ticket saved = new Ticket();
        saved.setId(ticketId);
        saved.setTitle("Bug");
        saved.setStatus(TicketStatus.OPEN);
        saved.setCreatedBy(user);

        // Stub the repositories
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(ticketRepository.save(any())).thenReturn(saved);

        // Act
        TicketResponse result = ticketService.createTicket(new TicketCreateRequest("Bug", null), "test@test.com");

        // Assert: id and initial status match
        assertThat(result.id()).isEqualTo(ticketId);
        assertThat(result.status()).isEqualTo(TicketStatus.OPEN);
    }

    /**
     * Verifies that createTicket throws ResourceNotFoundException
     * when the email does not match any user.
     */
    @Test
    void createTicket_withUnknownUser_throwsResourceNotFoundException() {
        // Arrange: no user found for this email
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
            ticketService.createTicket(new TicketCreateRequest("Bug", null), "ghost@test.com")
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that getTicketById returns the correct TicketResponse
     * when the ticket belongs to the caller.
     */
    @Test
    void getTicketById_withValidId_returnsTicketResponse() {
        // Arrange: ticket belonging to the calling user
        String ticketId = "t1";

        User user = new User();
        user.setId("u1");
        user.setEmail("user@test.com");
        user.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTitle("Bug");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(user);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        // Act
        TicketResponse result = ticketService.getTicketById(ticketId, "user@test.com");

        // Assert
        assertThat(result.id()).isEqualTo(ticketId);
    }

    /**
     * Verifies that getTicketById throws ResourceNotFoundException
     * when the id is unknown.
     */
    @Test
    void getTicketById_withUnknownId_throwsResourceNotFoundException() {
        // Arrange: missing ticket
        String ticketId = "t1";
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketService.getTicketById(ticketId, "caller@test.com"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that a USER cannot read another user's ticket.
     */
    @Test
    void getTicketById_asNonOwnerUser_throwsAccessDenied() {
        // Arrange: ticket belongs to "u1", caller is "u2" (USER role)
        String ticketId = "t1";

        User owner = new User();
        owner.setId("u1");

        User caller = new User();
        caller.setId("u2");
        caller.setEmail("other@test.com");
        caller.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(owner);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(caller));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.getTicketById(ticketId, "other@test.com"))
            .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Verifies that an AGENT can perform a valid OPEN → IN_PROGRESS transition.
     */
    @Test
    void transition_fromOpen_toInProgress_succeeds() {
        // Arrange: AGENT actor (authorized for any transition)
        String ticketId = "t1";

        User agent = new User();
        agent.setId("u1");
        agent.setEmail("agent@test.com");
        agent.setRole(Role.AGENT);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(agent);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(ticketRepository.save(any())).thenReturn(ticket);

        // Act
        TicketResponse result = ticketService.transition(ticketId, TicketStatus.IN_PROGRESS, "agent@test.com");

        // Assert: the status has changed
        assertThat(result.status()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    /**
     * Verifies that an invalid OPEN → CLOSED transition (by an AGENT, so
     * authorized) throws InvalidTransitionException.
     */
    @Test
    void transition_invalidTransition_throwsInvalidTransitionException() {
        // Arrange: AGENT actor to pass the access control check
        String ticketId = "t1";

        User agent = new User();
        agent.setId("u1");
        agent.setEmail("agent@test.com");
        agent.setRole(Role.AGENT);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.OPEN); // OPEN cannot go directly to CLOSED
        ticket.setCreatedBy(agent);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.transition(ticketId, TicketStatus.CLOSED, "agent@test.com"))
            .isInstanceOf(InvalidTransitionException.class);
    }

    /**
     * Verifies that a USER canNOT perform a transition other than
     * IN_PROGRESS → RESOLVED (here OPEN → IN_PROGRESS): AccessDeniedException.
     */
    @Test
    void transition_asUser_forbiddenTransition_throwsAccessDenied() {
        // Arrange: USER actor
        String ticketId = "t1";

        User user = new User();
        user.setId("u1");
        user.setEmail("user@test.com");
        user.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(user);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.transition(ticketId, TicketStatus.IN_PROGRESS, "user@test.com"))
            .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Verifies that a USER can resolve a ticket in progress
     * (IN_PROGRESS → RESOLVED).
     */
    @Test
    void transition_asUser_resolveInProgress_succeeds() {
        // Arrange: USER actor, IN_PROGRESS ticket
        String ticketId = "t1";

        User user = new User();
        user.setId("u1");
        user.setEmail("user@test.com");
        user.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setCreatedBy(user);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(ticketRepository.save(any())).thenReturn(ticket);

        // Act
        TicketResponse result = ticketService.transition(ticketId, TicketStatus.RESOLVED, "user@test.com");

        // Assert
        assertThat(result.status()).isEqualTo(TicketStatus.RESOLVED);
    }

    /**
     * Verifies that a USER canNOT resolve the IN_PROGRESS ticket
     * of another user (they are not its creator): AccessDeniedException.
     */
    @Test
    void transition_asNonCreatorUser_throwsAccessDenied() {
        // Arrange: the ticket belongs to another user
        String ticketId = "t1";

        User creator = new User();
        creator.setId("u1");

        User other = new User();
        other.setId("u2");
        other.setEmail("other@test.com");
        other.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setCreatedBy(creator);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(other));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.transition(ticketId, TicketStatus.RESOLVED, "other@test.com"))
            .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Verifies that getTicketsByUserId returns the user's tickets
     * when the caller queries their own id.
     */
    @Test
    void getTicketsByUserId_withValidUser_returnsTickets() {
        // Arrange: a USER querying their own tickets
        String userId = "u1";
        String ticketId = "t1";

        User user = new User();
        user.setId(userId);
        user.setEmail("user@test.com");
        user.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(user);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(ticketRepository.findByCreatedBy_Id(userId)).thenReturn(List.of(ticket));

        // Act
        List<TicketResponse> result = ticketService.getTicketsByUserId(userId, "user@test.com");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(ticketId);
    }

    /**
     * Verifies that getTicketsByUserId throws ResourceNotFoundException
     * when the target user does not exist (caller is ADMIN).
     */
    @Test
    void getTicketsByUserId_withUnknownUser_throwsResourceNotFoundException() {
        // Arrange: admin looking up a nonexistent user
        String userId = "u1";

        User admin = new User();
        admin.setId("admin-id");
        admin.setEmail("admin@test.com");
        admin.setRole(Role.ADMIN);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(userRepository.existsById(userId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> ticketService.getTicketsByUserId(userId, "admin@test.com"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that a USER cannot list another user's tickets.
     */
    @Test
    void getTicketsByUserId_asNonOwnerUser_throwsAccessDenied() {
        // Arrange: USER "u2" trying to access "u1"'s tickets
        String targetUserId = "u1";

        User caller = new User();
        caller.setId("u2");
        caller.setEmail("other@test.com");
        caller.setRole(Role.USER);

        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(caller));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.getTicketsByUserId(targetUserId, "other@test.com"))
            .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Verifies that a USER only retrieves the tickets they created.
     */
    @Test
    void getVisibleTickets_asUser_returnsOnlyOwnTickets() {
        // Arrange: user with the USER role
        String userId = "u1";
        String ticketId = "t1";

        User user = new User();
        user.setId(userId);
        user.setEmail("user@test.com");
        user.setRole(Role.USER);

        Ticket own = new Ticket();
        own.setId(ticketId);
        own.setStatus(TicketStatus.OPEN);
        own.setCreatedBy(user);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(ticketRepository.findByCreatedBy_Id(userId)).thenReturn(List.of(own));

        // Act
        List<TicketResponse> result = ticketService.getVisibleTickets("user@test.com");

        // Assert: only their tickets, not a findAll
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(ticketId);
    }

    /**
     * Verifies that an AGENT (or ADMIN) retrieves all tickets.
     */
    @Test
    void getVisibleTickets_asAgent_returnsAllTickets() {
        // Arrange: user with the AGENT role
        User agent = new User();
        agent.setId("u1");
        agent.setEmail("agent@test.com");
        agent.setRole(Role.AGENT);

        Ticket t1 = new Ticket();
        t1.setId("t1");
        t1.setStatus(TicketStatus.OPEN);
        t1.setCreatedBy(agent);

        Ticket t2 = new Ticket();
        t2.setId("t2");
        t2.setStatus(TicketStatus.OPEN);
        t2.setCreatedBy(agent);

        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(ticketRepository.findAll()).thenReturn(List.of(t1, t2));

        // Act
        List<TicketResponse> result = ticketService.getVisibleTickets("agent@test.com");

        // Assert: all tickets
        assertThat(result).hasSize(2);
    }

    /**
     * Verifies that getVisibleTickets throws ResourceNotFoundException
     * when the email does not match any user.
     */
    @Test
    void getVisibleTickets_withUnknownUser_throwsResourceNotFoundException() {
        // Arrange: no user for this email
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketService.getVisibleTickets("ghost@test.com"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that a ticket's creator can update its title and description.
     */
    @Test
    void updateTicket_asOwner_updatesAndReturnsResponse() {
        // Arrange: ticket belonging to the caller
        String ticketId = "t1";

        User owner = new User();
        owner.setId("u1");
        owner.setEmail("user@test.com");
        owner.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTitle("Old title");
        ticket.setDescription("Old desc");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(owner);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(owner));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        TicketResponse result = ticketService.updateTicket(ticketId,
            new TicketUpdateRequest("New title", "New desc"), "user@test.com");

        // Assert
        assertThat(result.title()).isEqualTo("New title");
    }

    /**
     * Verifies that a USER cannot update another user's ticket.
     */
    @Test
    void updateTicket_asNonOwnerUser_throwsAccessDenied() {
        // Arrange: ticket belongs to "u1", caller is "u2" (USER role)
        String ticketId = "t1";

        User owner = new User();
        owner.setId("u1");

        User caller = new User();
        caller.setId("u2");
        caller.setEmail("other@test.com");
        caller.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(owner);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(caller));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.updateTicket(ticketId,
            new TicketUpdateRequest("X", "Y"), "other@test.com"))
            .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Verifies that updateTicket throws ResourceNotFoundException
     * when the ticket does not exist.
     */
    @Test
    void updateTicket_withUnknownTicket_throwsResourceNotFoundException() {
        // Arrange: missing ticket
        String ticketId = "t1";
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketService.updateTicket(ticketId,
            new TicketUpdateRequest("X", "Y"), "user@test.com"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that assignTicket assigns the ticket to an AGENT
     * when the ticket and the user exist.
     */
    @Test
    void assignTicket_withValidTicketAndAgent_returnsResponse() {
        // Arrange
        String ticketId = "t1";
        String assigneeId = "u1";

        User creator = new User();
        creator.setId("u1");

        User assignee = new User();
        assignee.setId(assigneeId);
        assignee.setName("Agent Smith");
        assignee.setRole(Role.AGENT);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(creator);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        TicketResponse result = ticketService.assignTicket(ticketId, assigneeId);

        // Assert: the ticket is correctly assigned
        assertThat(result.assignedToId()).isEqualTo(assigneeId);
        assertThat(result.assignedToName()).isEqualTo("Agent Smith");
    }

    /**
     * Verifies that assignTicket throws InvalidAssigneeException
     * when the target user has the USER role (not authorized).
     */
    @Test
    void assignTicket_withUserRoleAssignee_throwsInvalidAssigneeException() {
        // Arrange: assignee is a plain USER
        String ticketId = "t1";
        String assigneeId = "u2";

        User assignee = new User();
        assignee.setId(assigneeId);
        assignee.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.OPEN);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.assignTicket(ticketId, assigneeId))
            .isInstanceOf(InvalidAssigneeException.class);
    }

    /**
     * Verifies that assignTicket throws ResourceNotFoundException
     * when the ticket does not exist.
     */
    @Test
    void assignTicket_withUnknownTicket_throwsResourceNotFoundException() {
        // Arrange: missing ticket
        String ticketId = "t1";
        String assigneeId = "u1";
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketService.assignTicket(ticketId, assigneeId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that assignTicket throws ResourceNotFoundException
     * when the target user does not exist.
     */
    @Test
    void assignTicket_withUnknownUser_throwsResourceNotFoundException() {
        // Arrange: ticket present but user missing
        String ticketId = "t1";
        String assigneeId = "u1";

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.OPEN);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketService.assignTicket(ticketId, assigneeId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that deleteTicket throws ResourceNotFoundException
     * when the ticket to delete does not exist.
     */
    @Test
    void deleteTicket_withUnknownId_throwsResourceNotFoundException() {
        // Arrange: the ticket does not exist
        String ticketId = "t1";
        when(ticketRepository.existsById(ticketId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> ticketService.deleteTicket(ticketId))
            .isInstanceOf(ResourceNotFoundException.class);

        // Verify that existsById was indeed called (no unnecessary deletion)
        verify(ticketRepository).existsById(ticketId);
    }
}

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

import com.helpdesk.backend.Data_Transfert_Object.TicketCreateRequest;
import com.helpdesk.backend.Data_Transfert_Object.TicketResponse;
import com.helpdesk.backend.exception.InvalidTransitionException;
import com.helpdesk.backend.exception.ResourceNotFoundException;
import com.helpdesk.backend.model.Ticket;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.model.enums.Role;
import com.helpdesk.backend.model.enums.Ticketstatus;
import com.helpdesk.backend.repository.TicketRepository;
import com.helpdesk.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    // Mocks des dépendances injectées dans TicketService
    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private TicketService ticketService;

    /**
     * Vérifie que createTicket retourne un TicketResponse valide
     * lorsque l'utilisateur existe en base de données.
     */
    @Test
    void createTicket_withValidUser_returnsTicketResponse() {
        // Arrange : utilisateur existant
        User user = new User();
        user.setId("u1");
        user.setEmail("test@test.com");

        // Arrange : ticket retourné après sauvegarde
        Ticket saved = new Ticket();
        saved.setId("t1");
        saved.setTitle("Bug");
        saved.setStatus(Ticketstatus.OPEN);
        saved.setCreatedBy(user);

        // Stubbing des repositories
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(ticketRepository.save(any())).thenReturn(saved);

        // Act
        TicketResponse result = ticketService.createTicket(new TicketCreateRequest("Bug", null), "test@test.com");

        // Assert : id et statut initial correspondent
        assertThat(result.id()).isEqualTo("t1");
        assertThat(result.status()).isEqualTo(Ticketstatus.OPEN);
    }

    /**
     * Vérifie que createTicket lève ResourceNotFoundException
     * lorsque l'email ne correspond à aucun utilisateur.
     */
    @Test
    void createTicket_withUnknownUser_throwsResourceNotFoundException() {
        // Arrange : aucun utilisateur trouvé pour cet email
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
            ticketService.createTicket(new TicketCreateRequest("Bug", null), "ghost@test.com")
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Vérifie que getTicketById retourne le bon TicketResponse
     * lorsque le ticket existe.
     */
    @Test
    void getTicketById_withValidId_returnsTicketResponse() {
        // Arrange
        User user = new User();
        user.setId("u1");

        Ticket ticket = new Ticket();
        ticket.setId("t1");
        ticket.setTitle("Bug");
        ticket.setStatus(Ticketstatus.OPEN);
        ticket.setCreatedBy(user);

        when(ticketRepository.findById("t1")).thenReturn(Optional.of(ticket));

        // Act
        TicketResponse result = ticketService.getTicketById("t1");

        // Assert
        assertThat(result.id()).isEqualTo("t1");
    }

    /**
     * Vérifie que getTicketById lève ResourceNotFoundException
     * lorsque l'identifiant est inconnu.
     */
    @Test
    void getTicketById_withUnknownId_throwsResourceNotFoundException() {
        // Arrange : ticket absent
        when(ticketRepository.findById("bad")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketService.getTicketById("bad"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Vérifie qu'un AGENT peut faire une transition valide OPEN → IN_PROGRESS.
     */
    @Test
    void transition_fromOpen_toInProgress_succeeds() {
        // Arrange : acteur AGENT (autorisé pour toute transition)
        User agent = new User();
        agent.setId("a1");
        agent.setEmail("agent@test.com");
        agent.setRole(Role.AGENT);

        Ticket ticket = new Ticket();
        ticket.setId("t1");
        ticket.setStatus(Ticketstatus.OPEN);
        ticket.setCreatedBy(agent);

        when(ticketRepository.findById("t1")).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(ticketRepository.save(any())).thenReturn(ticket);

        // Act
        TicketResponse result = ticketService.transition("t1", Ticketstatus.IN_PROGRESS, "agent@test.com");

        // Assert : le statut a bien changé
        assertThat(result.status()).isEqualTo(Ticketstatus.IN_PROGRESS);
    }

    /**
     * Vérifie qu'une transition invalide OPEN → CLOSED (par un AGENT, donc
     * autorisé) lève InvalidTransitionException.
     */
    @Test
    void transition_invalidTransition_throwsInvalidTransitionException() {
        // Arrange : acteur AGENT pour franchir le contrôle d'accès
        User agent = new User();
        agent.setId("a1");
        agent.setEmail("agent@test.com");
        agent.setRole(Role.AGENT);

        Ticket ticket = new Ticket();
        ticket.setId("t1");
        ticket.setStatus(Ticketstatus.OPEN); // OPEN ne peut pas aller directement à CLOSED
        ticket.setCreatedBy(agent);

        when(ticketRepository.findById("t1")).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.transition("t1", Ticketstatus.CLOSED, "agent@test.com"))
            .isInstanceOf(InvalidTransitionException.class);
    }

    /**
     * Vérifie qu'un USER ne peut PAS faire une transition autre que
     * IN_PROGRESS → RESOLVED (ici OPEN → IN_PROGRESS) : AccessDeniedException.
     */
    @Test
    void transition_asUser_forbiddenTransition_throwsAccessDenied() {
        // Arrange : acteur USER
        User user = new User();
        user.setId("u1");
        user.setEmail("user@test.com");
        user.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId("t1");
        ticket.setStatus(Ticketstatus.OPEN);
        ticket.setCreatedBy(user);

        when(ticketRepository.findById("t1")).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.transition("t1", Ticketstatus.IN_PROGRESS, "user@test.com"))
            .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Vérifie qu'un USER peut résoudre un ticket en cours
     * (IN_PROGRESS → RESOLVED).
     */
    @Test
    void transition_asUser_resolveInProgress_succeeds() {
        // Arrange : acteur USER, ticket IN_PROGRESS
        User user = new User();
        user.setId("u1");
        user.setEmail("user@test.com");
        user.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId("t1");
        ticket.setStatus(Ticketstatus.IN_PROGRESS);
        ticket.setCreatedBy(user);

        when(ticketRepository.findById("t1")).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(ticketRepository.save(any())).thenReturn(ticket);

        // Act
        TicketResponse result = ticketService.transition("t1", Ticketstatus.RESOLVED, "user@test.com");

        // Assert
        assertThat(result.status()).isEqualTo(Ticketstatus.RESOLVED);
    }

    /**
     * Vérifie qu'un USER ne peut PAS résoudre le ticket IN_PROGRESS
     * d'un autre utilisateur (il n'en est pas le créateur) : AccessDeniedException.
     */
    @Test
    void transition_asNonCreatorUser_throwsAccessDenied() {
        // Arrange : le ticket appartient à un autre utilisateur
        User creator = new User();
        creator.setId("owner");

        User other = new User();
        other.setId("u2");
        other.setEmail("other@test.com");
        other.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId("t1");
        ticket.setStatus(Ticketstatus.IN_PROGRESS);
        ticket.setCreatedBy(creator);

        when(ticketRepository.findById("t1")).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(other));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.transition("t1", Ticketstatus.RESOLVED, "other@test.com"))
            .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Vérifie que getTicketsByUserId retourne les tickets de l'utilisateur
     * lorsque celui-ci existe.
     */
    @Test
    void getTicketsByUserId_withValidUser_returnsTickets() {
        // Arrange : utilisateur existant avec un ticket
        User user = new User();
        user.setId("u1");

        Ticket ticket = new Ticket();
        ticket.setId("t1");
        ticket.setStatus(Ticketstatus.OPEN);
        ticket.setCreatedBy(user);

        when(userRepository.existsById("u1")).thenReturn(true);
        when(ticketRepository.findByCreatedBy_Id("u1")).thenReturn(List.of(ticket));

        // Act
        List<TicketResponse> result = ticketService.getTicketsByUserId("u1");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("t1");
    }

    /**
     * Vérifie que getTicketsByUserId lève ResourceNotFoundException
     * lorsque l'utilisateur n'existe pas.
     */
    @Test
    void getTicketsByUserId_withUnknownUser_throwsResourceNotFoundException() {
        // Arrange : utilisateur absent
        when(userRepository.existsById("bad")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> ticketService.getTicketsByUserId("bad"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Vérifie qu'un USER ne récupère que les tickets qu'il a créés.
     */
    @Test
    void getVisibleTickets_asUser_returnsOnlyOwnTickets() {
        // Arrange : utilisateur avec le rôle USER
        User user = new User();
        user.setId("u1");
        user.setEmail("user@test.com");
        user.setRole(Role.USER);

        Ticket own = new Ticket();
        own.setId("t1");
        own.setStatus(Ticketstatus.OPEN);
        own.setCreatedBy(user);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(userRepository.existsById("u1")).thenReturn(true);
        when(ticketRepository.findByCreatedBy_Id("u1")).thenReturn(List.of(own));

        // Act
        List<TicketResponse> result = ticketService.getVisibleTickets("user@test.com");

        // Assert : seuls ses tickets, pas un findAll
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("t1");
    }

    /**
     * Vérifie qu'un AGENT (ou ADMIN) récupère tous les tickets.
     */
    @Test
    void getVisibleTickets_asAgent_returnsAllTickets() {
        // Arrange : utilisateur avec le rôle AGENT
        User agent = new User();
        agent.setId("a1");
        agent.setEmail("agent@test.com");
        agent.setRole(Role.AGENT);

        Ticket t1 = new Ticket();
        t1.setId("t1");
        t1.setStatus(Ticketstatus.OPEN);
        t1.setCreatedBy(agent);

        Ticket t2 = new Ticket();
        t2.setId("t2");
        t2.setStatus(Ticketstatus.OPEN);
        t2.setCreatedBy(agent);

        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(ticketRepository.findAll()).thenReturn(List.of(t1, t2));

        // Act
        List<TicketResponse> result = ticketService.getVisibleTickets("agent@test.com");

        // Assert : tous les tickets
        assertThat(result).hasSize(2);
    }

    /**
     * Vérifie que getVisibleTickets lève ResourceNotFoundException
     * lorsque l'email ne correspond à aucun utilisateur.
     */
    @Test
    void getVisibleTickets_withUnknownUser_throwsResourceNotFoundException() {
        // Arrange : aucun utilisateur pour cet email
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketService.getVisibleTickets("ghost@test.com"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Vérifie que assignTicket affecte le ticket à l'utilisateur cible
     * lorsque le ticket et l'utilisateur existent.
     */
    @Test
    void assignTicket_withValidTicketAndUser_returnsResponse() {
        // Arrange
        User creator = new User();
        creator.setId("u1");

        User assignee = new User();
        assignee.setId("a1");
        assignee.setName("Agent Smith");

        Ticket ticket = new Ticket();
        ticket.setId("t1");
        ticket.setStatus(Ticketstatus.OPEN);
        ticket.setCreatedBy(creator);

        when(ticketRepository.findById("t1")).thenReturn(Optional.of(ticket));
        when(userRepository.findById("a1")).thenReturn(Optional.of(assignee));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        TicketResponse result = ticketService.assignTicket("t1", "a1");

        // Assert : le ticket est bien assigné
        assertThat(result.assignedToId()).isEqualTo("a1");
        assertThat(result.assignedToName()).isEqualTo("Agent Smith");
    }

    /**
     * Vérifie que assignTicket lève ResourceNotFoundException
     * lorsque le ticket n'existe pas.
     */
    @Test
    void assignTicket_withUnknownTicket_throwsResourceNotFoundException() {
        // Arrange : ticket absent
        when(ticketRepository.findById("bad")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketService.assignTicket("bad", "a1"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Vérifie que assignTicket lève ResourceNotFoundException
     * lorsque l'utilisateur cible n'existe pas.
     */
    @Test
    void assignTicket_withUnknownUser_throwsResourceNotFoundException() {
        // Arrange : ticket présent mais utilisateur absent
        Ticket ticket = new Ticket();
        ticket.setId("t1");

        when(ticketRepository.findById("t1")).thenReturn(Optional.of(ticket));
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> ticketService.assignTicket("t1", "ghost"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Vérifie que deleteTicket lève ResourceNotFoundException
     * lorsque le ticket à supprimer n'existe pas.
     */
    @Test
    void deleteTicket_withUnknownId_throwsResourceNotFoundException() {
        // Arrange : le ticket n'existe pas
        when(ticketRepository.existsById("bad")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> ticketService.deleteTicket("bad"))
            .isInstanceOf(ResourceNotFoundException.class);

        // Vérifie que existsById a bien été appelé (pas de suppression inutile)
        verify(ticketRepository).existsById("bad");
    }
}

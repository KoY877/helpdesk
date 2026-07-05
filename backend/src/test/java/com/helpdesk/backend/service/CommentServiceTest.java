package com.helpdesk.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.helpdesk.backend.model.enums.Role;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.helpdesk.backend.dto.CommentCreateRequest;
import com.helpdesk.backend.dto.CommentResponse;
import com.helpdesk.backend.exception.ResourceNotFoundException;
import com.helpdesk.backend.model.Comment;
import com.helpdesk.backend.model.Ticket;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.repository.CommentRepository;
import com.helpdesk.backend.repository.TicketRepository;
import com.helpdesk.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {
    @Mock private CommentRepository commentRespository;
    @Mock private TicketRepository ticketRespository;
    @Mock private UserRepository userRepository;
    @InjectMocks private CommentService commentService;

    /**
     * Verifies that a ticket's creator can post a comment on it.
     */
    @Test
    void createComment_withValidData_returnsCommentResponse() {
        // Arrange: user who owns the ticket
        String ticketId = "t1";
        String userId = "u1";

        User user = new User();
        user.setId(userId);
        user.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        // The ticket must know its creator so the ownership check passes
        ticket.setCreatedBy(user);

        Comment comment = new Comment();
        comment.setId("c1");
        comment.setContent("Lorem Ipsummmmmm");
        comment.setAuthor(user);
        comment.setTicket(ticket);

        when(ticketRespository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("user1@test.com")).thenReturn(Optional.of(user));
        when(commentRespository.save(any())).thenReturn(comment);

        CommentCreateRequest request = new CommentCreateRequest("Lorem Ipsummmmmm");
        CommentResponse result = commentService.createComment(ticketId, "user1@test.com", request);

        assertThat(result.content()).isEqualTo("Lorem Ipsummmmmm");
    }

    /**
     * Verifies that createComment throws ResourceNotFoundException
     * when the target ticket does not exist.
     */
    @Test
    void createComment_withUnknownTicket_throwsResourceNotFoundException() {
        String ticketId = "t1";
        when(ticketRespository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> {
            CommentCreateRequest request = new CommentCreateRequest("c");
            commentService.createComment(ticketId, "user2@test.com", request);
        }).isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that a USER cannot comment on another user's ticket.
     */
    @Test
    void createComment_asNonOwnerUser_throwsAccessDenied() {
        // Arrange: ticket belongs to "u1", caller is "u2" (USER role)
        String ticketId = "t1";

        User owner = new User();
        owner.setId("u1");

        User caller = new User();
        caller.setId("u2");
        caller.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setCreatedBy(owner);

        when(ticketRespository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(caller));

        assertThatThrownBy(() ->
            commentService.createComment(ticketId, "other@test.com", new CommentCreateRequest("x"))
        ).isInstanceOf(AccessDeniedException.class);
    }

    /**
     * Verifies that getCommentsByTicket returns the ticket's comments
     * when the caller is an AGENT authorized to see everything.
     */
    @Test
    void getCommentsByTicket_withExistingTicket_returnsMappedComments() {
        // Arrange: AGENT reading the comments of an existing ticket
        String ticketId = "t1";

        User owner = new User();
        owner.setId("owner-id");

        User agent = new User();
        agent.setId("u1");
        agent.setEmail("agent@test.com");
        agent.setName("Kodjo");
        agent.setRole(Role.AGENT);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setCreatedBy(owner);

        Comment comment = new Comment();
        comment.setId("c1");
        comment.setContent("Looking into it now");
        comment.setAuthor(agent);
        comment.setTicket(ticket);

        when(ticketRespository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agent));
        when(commentRespository.findByTicket_IdOrderByCreatedAtAsc(ticketId))
            .thenReturn(List.of(comment));

        // Act
        List<CommentResponse> result = commentService.getCommentsByTicket(ticketId, "agent@test.com");

        // Assert : the comment is mapped with its author's name and role
        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).isEqualTo("Looking into it now");
        assertThat(result.get(0).authorName()).isEqualTo("Kodjo");
        assertThat(result.get(0).authorRole()).isEqualTo("AGENT");
    }

    /**
     * Verifies that getCommentsByTicket throws ResourceNotFoundException
     * when the ticket does not exist.
     */
    @Test
    void getCommentsByTicket_withUnknownTicket_throwsResourceNotFoundException() {
        String ticketId = "t1";
        when(ticketRespository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getCommentsByTicket(ticketId, "caller@test.com"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * Verifies that a USER cannot read the comments of another user's ticket.
     */
    @Test
    void getCommentsByTicket_asNonOwnerUser_throwsAccessDenied() {
        // Arrange: ticket belongs to "u1", caller is "u2" (USER role)
        String ticketId = "t1";

        User owner = new User();
        owner.setId("u1");

        User caller = new User();
        caller.setId("u2");
        caller.setRole(Role.USER);

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setCreatedBy(owner);

        when(ticketRespository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(caller));

        assertThatThrownBy(() -> commentService.getCommentsByTicket(ticketId, "other@test.com"))
            .isInstanceOf(AccessDeniedException.class);
    }
}

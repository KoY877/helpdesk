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

import com.helpdesk.backend.Data_Transfert_Object.CommentCreateRequest;
import com.helpdesk.backend.Data_Transfert_Object.CommentResponse;
import com.helpdesk.backend.exception.ResourceNotFoundException;
import com.helpdesk.backend.model.Comment;
import com.helpdesk.backend.model.Ticket;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.repository.CommentRespository;
import com.helpdesk.backend.repository.TicketRepository;
import com.helpdesk.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {
    // Mocks des dépendances injectées dans TicketService
    @Mock private CommentRespository commentRespository;
    @Mock private TicketRepository ticketRespository;
    @Mock private UserRepository userRepository;
    @InjectMocks private CommentService commentService;

    @Test 
    void createComment_withValidData_returnsCommentResponse(){
         // Arrange : utilisateur existant
        User user = new User();
        user.setId("u1");

        Ticket ticket = new Ticket();
        ticket.setId("t1");

        Comment comment = new Comment();
        comment.setId("c1");
        comment.setContent("Lorem Ipsummmmmm");
        comment.setAuthor(user);
        comment.setTicket(ticket);

        when(ticketRespository.findById("t1")).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("user1@test.com")).thenReturn(Optional.of(user));
        when(commentRespository.save(any())).thenReturn(comment);

        CommentCreateRequest request = new CommentCreateRequest("Lorem Ipsummmmmm");
        CommentResponse result = commentService.createComment("t1", "user1@test.com", request);

        assertThat(result.content()).isEqualTo("Lorem Ipsummmmmm");
     
    };

    @Test
    void createComment_withUnknownTicket_throwsResourceNotFoundException(){
        when(ticketRespository.findById("t2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> {
            CommentCreateRequest request = new CommentCreateRequest("c");
            commentService.createComment("t2", "user2@test.com", request);
        }).isInstanceOf(ResourceNotFoundException.class);

    };

    @Test
    void getCommentsByTicket_withExistingTicket_returnsMappedComments(){
        // Arrange : an existing ticket with one comment authored by a user
        User author = new User();
        author.setId("u1");
        author.setName("Kodjo");
        author.setRole(Role.AGENT);

        Ticket ticket = new Ticket();
        ticket.setId("t1");

        Comment comment = new Comment();
        comment.setId("c1");
        comment.setContent("Looking into it now");
        comment.setAuthor(author);
        comment.setTicket(ticket);

        when(ticketRespository.existsById("t1")).thenReturn(true);
        when(commentRespository.findByTicket_IdOrderByCreatedAtAsc("t1"))
            .thenReturn(List.of(comment));

        // Act
        List<CommentResponse> result = commentService.getCommentsByTicket("t1");

        // Assert : the comment is mapped with its author's name and role
        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).isEqualTo("Looking into it now");
        assertThat(result.get(0).authorName()).isEqualTo("Kodjo");
        assertThat(result.get(0).authorRole()).isEqualTo("AGENT");
    };

    @Test
    void getCommentsByTicket_withUnknownTicket_throwsResourceNotFoundException(){
        when(ticketRespository.existsById("t2")).thenReturn(false);

        assertThatThrownBy(() -> commentService.getCommentsByTicket("t2"))
            .isInstanceOf(ResourceNotFoundException.class);
    };
}

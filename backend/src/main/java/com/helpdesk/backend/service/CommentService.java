package com.helpdesk.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import com.helpdesk.backend.dto.CommentCreateRequest;
import com.helpdesk.backend.dto.CommentMapper;
import com.helpdesk.backend.dto.CommentResponse;
import com.helpdesk.backend.exception.ResourceNotFoundException;
import com.helpdesk.backend.model.Comment;
import com.helpdesk.backend.model.Ticket;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.repository.CommentRepository;
import com.helpdesk.backend.repository.TicketRepository;
import com.helpdesk.backend.repository.UserRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class CommentService {

   
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

  
    /**
     * Returns every comment attached to a ticket, oldest first.
     *
     * @param ticketId the unique identifier of the ticket
     * @return the ticket's comments as {@link CommentResponse} DTOs
     * @throws ResourceNotFoundException if the ticket does not exist
     */
    @Transactional
    public List<CommentResponse> getCommentsByTicket(@NotNull String ticketId) {
        // Fail fast if the ticket does not exist
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket not found: " + ticketId);
        }

        // Fetch the ticket's comments and map each one to its response DTO
        return commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId)
            .stream()
            .map(CommentMapper::toResponse)
            .toList();
    }

    /**
     * Creates a new comment on a ticket and persists it.
     *
     * @param ticketId    the unique identifier of the ticket being commented on
     * @param authorEmail the email of the comment's author
     * @param request     the comment creation request containing the content
     * @return the saved comment as a {@link CommentResponse}
     * @throws ResourceNotFoundException if the ticket or the author does not exist
     */
    @Transactional
    public CommentResponse createComment (String ticketId, String authorEmail, @NotNull CommentCreateRequest request) {
        // Resolve the target ticket or throw if it is missing
        Ticket ticket = ticketRepository.findById(ticketId)
                        .orElseThrow(()-> new ResourceNotFoundException("Ticket not found: "+ ticketId));

        // Resolve the author from their email or throw if missing
        User author = userRepository.findByEmail(authorEmail)
                        .orElseThrow(()-> new ResourceNotFoundException("User not found"));

        // Build the comment linking it to the ticket and its author
        Comment comment = new Comment();
        comment.setContent(request.content());
        comment.setTicket(ticket);
        comment.setAuthor(author);

        // Save the comment to the database and return it as a response DTO
        return CommentMapper.toResponse(commentRepository.save(comment));
    }
   
}

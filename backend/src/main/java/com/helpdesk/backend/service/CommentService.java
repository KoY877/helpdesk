package com.helpdesk.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;

import com.helpdesk.backend.dto.CommentCreateRequest;
import com.helpdesk.backend.dto.CommentMapper;
import com.helpdesk.backend.dto.CommentResponse;
import com.helpdesk.backend.exception.ResourceNotFoundException;
import com.helpdesk.backend.model.Comment;
import com.helpdesk.backend.model.Ticket;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.model.enums.Role;
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
     * A USER may only read comments on a ticket they created; AGENTs and ADMINs see any.
     *
     * @param ticketId    the unique identifier of the ticket
     * @param callerEmail the email of the authenticated caller
     * @return the ticket's comments as {@link CommentResponse} DTOs
     * @throws ResourceNotFoundException if the ticket does not exist
     * @throws AccessDeniedException     if a USER tries to read comments on a ticket they do not own
     */
    @Transactional
    public List<CommentResponse> getCommentsByTicket(@NotNull String ticketId, @NotNull String callerEmail) {
        // Load the ticket so we can check ownership (existsById is not enough here)
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        // Resolve the caller to check their role
        User caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // A plain USER may only read comments on their own ticket
        if (caller.getRole() == Role.USER && !ticket.getCreatedBy().getId().equals(caller.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        // Fetch the ticket's comments and map each one to its response DTO
        return commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId)
            .stream()
            .map(CommentMapper::toResponse)
            .toList();
    }

    /**
     * Creates a new comment on a ticket and persists it.
     * A USER may only comment on a ticket they created; AGENTs and ADMINs may comment on any.
     *
     * @param ticketId    the unique identifier of the ticket being commented on
     * @param authorEmail the email of the comment's author
     * @param request     the comment creation request containing the content
     * @return the saved comment as a {@link CommentResponse}
     * @throws ResourceNotFoundException if the ticket or the author does not exist
     * @throws AccessDeniedException     if a USER tries to comment on a ticket they do not own
     */
    @Transactional
    public CommentResponse createComment(String ticketId, String authorEmail, @NotNull CommentCreateRequest request) {
        // Resolve the target ticket or throw if it is missing
        Ticket ticket = ticketRepository.findById(ticketId)
                        .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        // Resolve the author from their email or throw if missing
        User author = userRepository.findByEmail(authorEmail)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // A plain USER may only comment on their own ticket
        if (author.getRole() == Role.USER && !ticket.getCreatedBy().getId().equals(author.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        // Build the comment linking it to the ticket and its author
        Comment comment = new Comment();
        comment.setContent(request.content());
        comment.setTicket(ticket);
        comment.setAuthor(author);

        // Log only ids to avoid leaking PII, then persist and return
        log.info("Comment created by user id: {} on ticket id: {}", author.getId(), ticketId);
        return CommentMapper.toResponse(commentRepository.save(comment));
    }
   
}

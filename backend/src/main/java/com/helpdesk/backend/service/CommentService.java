package com.helpdesk.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.helpdesk.backend.Data_Transfert_Object.CommentCreateRequest;
import com.helpdesk.backend.Data_Transfert_Object.CommentMapper;
import com.helpdesk.backend.Data_Transfert_Object.CommentResponse;
import com.helpdesk.backend.exception.ResourceNotFoundException;
import com.helpdesk.backend.model.Comment;
import com.helpdesk.backend.model.Ticket;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.repository.CommentRespository;
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
    private final CommentRespository commentRespository;
    private final UserRepository userRepository;

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
        return CommentMapper.toResponse(commentRespository.save(comment));
    }
   
}

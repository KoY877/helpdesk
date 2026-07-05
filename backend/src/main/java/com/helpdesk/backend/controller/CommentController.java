package com.helpdesk.backend.controller;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;



import com.helpdesk.backend.dto.CommentCreateRequest;
import com.helpdesk.backend.dto.CommentResponse;
import com.helpdesk.backend.service.CommentService;

import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;

@RestController
@AllArgsConstructor
@Validated
public class CommentController {
    
    private final CommentService commentService;

    /**
     * Lists every comment attached to a ticket, oldest first.
     * A USER may only read comments on a ticket they created; AGENTs and ADMINs see any.
     *
     * @param ticketId    the unique identifier of the ticket
     * @param userDetails the currently authenticated user
     * @return HTTP 200 with the ticket's comments
     */
    @GetMapping("/api/tickets/{ticketId}/comments")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<List<CommentResponse>> getTicketComments(
        @PathVariable String ticketId,
        @AuthenticationPrincipal UserDetails userDetails) {

        // Pass the caller's identity so the service can enforce ownership
        return ResponseEntity.ok(commentService.getCommentsByTicket(ticketId, userDetails.getUsername()));
    }

    /**
     * Adds a comment to a ticket on behalf of the authenticated user.
     *
     * @param ticketId    the unique identifier of the ticket to comment on
     * @param request     the comment data (content)
     * @param userDetails the currently authenticated user (the author)
     * @return HTTP 201 with the created comment
     */
    @PostMapping("/api/tickets/{ticketId}/comments")
    @PreAuthorize("hasAnyRole('USER', 'AGENT', 'ADMIN')")
    public ResponseEntity<CommentResponse> createComment(
        @PathVariable String ticketId,
        @RequestBody @Valid CommentCreateRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {

        // Pass the author's email so the service can attach the comment's author
        return ResponseEntity.status(HttpStatus.CREATED)
        .body(commentService.createComment(ticketId, userDetails.getUsername(), request));
    }
}

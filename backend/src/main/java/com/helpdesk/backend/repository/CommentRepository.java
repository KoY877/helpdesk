package com.helpdesk.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.helpdesk.backend.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, String> {

    /**
     * Returns the comments attached to a ticket, oldest first.
     *
     * @param ticketId the id of the ticket whose comments are fetched
     * @return the ticket's comments ordered by creation date ascending
     */
    List<Comment> findByTicket_IdOrderByCreatedAtAsc(String ticketId);
}

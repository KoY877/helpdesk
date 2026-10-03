package com.helpdesk.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.helpdesk.backend.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, String> {
    // Retire les commentaires de l'utilisateur et ceux des tickets qui vont disparaître.
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from Comment c where c.author.id = :id or c.ticket.id in (select t.id from Ticket t where t.createdBy.id = :id)")
    void deleteForAccount(@org.springframework.data.repository.query.Param("id") String id);

    /**
     * Returns the comments attached to a ticket, oldest first.
     *
     * @param ticketId the id of the ticket whose comments are fetched
     * @return the ticket's comments ordered by creation date ascending
     */
    List<Comment> findByTicket_IdOrderByCreatedAtAsc(String ticketId);
}

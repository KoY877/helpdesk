package com.helpdesk.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.helpdesk.backend.model.Ticket;
import com.helpdesk.backend.model.User;

public interface TicketRepository extends JpaRepository<Ticket, String> {
    // Conserve les tickets des autres utilisateurs en supprimant seulement l'affectation.
    @org.springframework.data.jpa.repository.Modifying
    @Query("update Ticket t set t.assignedTo = null where t.assignedTo.id = :id")
    void unassignUser(@org.springframework.data.repository.query.Param("id") String id);

    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from Ticket t where t.createdBy.id = :id")
    void deleteCreatedByUser(@org.springframework.data.repository.query.Param("id") String id);

    List<Ticket> findByCreatedBy(User user);
    List<Ticket> findByCreatedBy_Id(String userId);
    List<Ticket> findByAssignedTo(User user);

    @Query(value = "SELECT COALESCE(MAX(ticket_order), 0) FROM tickets", nativeQuery = true)
    int findMaxOrder();
} 

package com.helpdesk.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.helpdesk.backend.model.Ticket;
import com.helpdesk.backend.model.User;

public interface TicketRepository extends JpaRepository<Ticket, String> {
    List<Ticket> findByCreatedBy(User user);
    List<Ticket> findByCreatedBy_Id(String userId);
    List<Ticket> findByAssignedTo(User user);

    @Query(value = "SELECT COALESCE(MAX(ticket_order), 0) FROM tickets", nativeQuery = true)
    int findMaxOrder();
} 

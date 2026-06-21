package com.helpdesk.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.helpdesk.backend.model.User;


public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query(value = "SELECT COALESCE(MAX(user_order), 0) FROM users", nativeQuery = true)
    int findMaxOrder();
} 

package com.helpdesk.backend.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.helpdesk.backend.model.RefreshToken;
import com.helpdesk.backend.model.User;

import jakarta.transaction.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String>  {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    void deleteByUser(User user);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from RefreshToken r where r.user.id = :id")
    void deleteForUser(@org.springframework.data.repository.query.Param("id") String id);

    @Modifying
    @Transactional
    int deleteByExpiryDateBefore(LocalDateTime date);

}

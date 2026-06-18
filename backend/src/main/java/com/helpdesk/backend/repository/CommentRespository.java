package com.helpdesk.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.helpdesk.backend.model.Comment;

public interface CommentRespository extends JpaRepository<Comment, String> {
}

package com.helpdesk.backend.dto;

import java.time.LocalDateTime;
public record CommentResponse (
    String id,
    String content,
    String ticketId,
    String authorId,
    String authorName,
    String authorRole,
    LocalDateTime createdAt
){

}

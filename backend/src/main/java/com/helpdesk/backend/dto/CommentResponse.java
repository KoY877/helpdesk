package com.helpdesk.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse (
    UUID id,
    String content,
    UUID ticketId,
    UUID authorId,
    String authorName,
    String authorRole,
    LocalDateTime createdAt
){

}

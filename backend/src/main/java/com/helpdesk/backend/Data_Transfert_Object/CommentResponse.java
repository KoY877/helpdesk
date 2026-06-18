package com.helpdesk.backend.Data_Transfert_Object;

import java.time.LocalDateTime;

public record CommentResponse (
    String id,
    String content,
    String ticketId,
    String authorId,
    LocalDateTime createdAt
){
    
}

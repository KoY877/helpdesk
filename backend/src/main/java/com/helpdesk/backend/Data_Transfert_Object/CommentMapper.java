package com.helpdesk.backend.Data_Transfert_Object;

import com.helpdesk.backend.model.Comment;

public class CommentMapper {
    /**
     * Maps a {@link Comment} entity to its {@link CommentResponse} DTO,
     * flattening the related ticket id and the author's id, name and role.
     *
     * @param comment the entity to map
     * @return the corresponding response DTO
     */
    public static CommentResponse toResponse (Comment comment) {
        // Expose the ticket by id and the author by id, name and role
        return new CommentResponse(
            comment.getId(),
            comment.getContent(),
            comment.getTicket().getId(),
            comment.getAuthor().getId(),
            comment.getAuthor().getName(),
            comment.getAuthor().getRole().name(),
            comment.getCreatedAt()
        );
    }
}

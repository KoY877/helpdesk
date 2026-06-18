package com.helpdesk.backend.Data_Transfert_Object;

import com.helpdesk.backend.model.Comment;

public class CommentMapper {
    /**
     * Maps a {@link Comment} entity to its {@link CommentResponse} DTO,
     * exposing only the ids of the related ticket and author.
     *
     * @param comment the entity to map
     * @return the corresponding response DTO
     */
    public static CommentResponse toResponse (Comment comment) {
        // Expose the related ticket and author by id only
        return new CommentResponse(
            comment.getId(),
            comment.getContent(),
            comment.getTicket().getId(),
            comment.getAuthor().getId(),
            comment.getCreatedAt()
        );
    }
}

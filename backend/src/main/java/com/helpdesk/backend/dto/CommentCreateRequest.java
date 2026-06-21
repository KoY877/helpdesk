package com.helpdesk.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest (
    @NotBlank String content

) {
    
}

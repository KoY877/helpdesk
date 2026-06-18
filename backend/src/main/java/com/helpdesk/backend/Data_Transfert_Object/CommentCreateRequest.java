package com.helpdesk.backend.Data_Transfert_Object;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest (
    @NotBlank String content

) {
    
}

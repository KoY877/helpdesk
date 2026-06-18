package com.helpdesk.backend.Data_Transfert_Object;

import com.helpdesk.backend.model.User;

public class UserMapper {
    /**
     * Maps a {@link User} entity to its public {@link UserResponse} DTO.
     * The password is intentionally never copied into the response.
     *
     * @param user the entity to map
     * @return the corresponding response DTO
     */
    public static UserResponse toResponse(User user) {
        // Copy only the fields safe to expose (password excluded)
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getOrder(),
                user.getCreatedAt()
        );
    }
}
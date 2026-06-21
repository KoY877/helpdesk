package com.helpdesk.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.backend.dto.UserMapper;
import com.helpdesk.backend.dto.UserResponse;
import com.helpdesk.backend.dto.UserRoleUpdateRequest;
import com.helpdesk.backend.dto.UserUpdateRequest;
import com.helpdesk.backend.exception.ResourceNotFoundException;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    

    /**
     * Retrieves every user stored in the system.
     *
     * @return the list of all users mapped to {@link UserResponse}
     * @throws ResourceNotFoundException if no user exists
     */
    @Transactional
    public List<UserResponse> getAllUsers() {
        // Load every user from the database
        List<User> users = userRepository.findAll();

        // Fail fast when the table is empty
        if (users.isEmpty()) {
            throw new ResourceNotFoundException("No users found");
        }

        // Map each entity to its response DTO
        return users.stream().map(UserMapper::toResponse).toList();
    }

    /**
     * Deletes the user identified by the given id.
     *
     * @param id the unique identifier of the user to delete
     * @throws ResourceNotFoundException if no user matches the id
     */
    @Transactional
    public void deleteUser(UUID id) {
        // Make sure the user exists before attempting to delete
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User: "+ id);
        }

        // Remove the user from the database
        userRepository.deleteById(id);
    }

    /**
     * Partially updates a user with the non-null fields of the request.
     *
     * @param id      the unique identifier of the user to update
     * @param request the fields to update (name, email and/or password)
     * @return the updated user as a {@link UserResponse}
     * @throws ResourceNotFoundException if no user matches the id
     */
    @Transactional
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        // Fetch the existing user or throw if absent
        User user = findOrThrow(id);

        // Only overwrite the fields that were provided
        if (request.name() != null) user.setName(request.name());
        if (request.email() != null) user.setEmail(request.email());
        // Hash the new password before storing it
        if (request.password() != null) user.setPassword(passwordEncoder.encode(request.password()));

        // Persist and return the updated user
        return UserMapper.toResponse(userRepository.save(user));
    }

    /**
     * Updates the role of an existing user.
     *
     * @param id      the unique identifier of the user
     * @param request the new role to assign
     * @return the updated user as a {@link UserResponse}
     * @throws ResourceNotFoundException if no user matches the id
     */
    @Transactional
    public UserResponse updateRole (UUID id, UserRoleUpdateRequest request) {
        // Fetch the existing user or throw if absent
        User user = findOrThrow(id);

        // Apply the new role
        user.setRole(request.role());

        // Persist and return the updated user
        return UserMapper.toResponse(userRepository.save(user));
    }

    /**
     * Finds a single user by id.
     *
     * @param id the unique identifier of the user
     * @return the matching user as a {@link UserResponse}
     * @throws ResourceNotFoundException if no user matches the id
     */
    public UserResponse findById(UUID id){
        // Delegate the lookup and map the result to a DTO
        return UserMapper.toResponse(findOrThrow(id));
    }

    /**
     * Loads a user entity by id or throws if it does not exist.
     *
     * @param id the unique identifier of the user
     * @return the matching {@link User} entity
     * @throws ResourceNotFoundException if no user matches the id
     */
    private User findOrThrow(UUID id){
        // Return the user or raise a not-found exception
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}

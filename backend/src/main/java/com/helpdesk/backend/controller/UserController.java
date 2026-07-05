package com.helpdesk.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.helpdesk.backend.dto.UserResponse;
import com.helpdesk.backend.dto.UserRoleUpdateRequest;
import com.helpdesk.backend.dto.UserUpdateRequest;
import com.helpdesk.backend.service.UserService;

import lombok.AllArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;




@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
@Validated
public class UserController {
    
    private final UserService userService;


    /**
     * Retrieves all users in the system.
     *
     * @return a {@link ResponseEntity} containing a list of all {@link User} objects
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        // Delegate to the service and return the full list of users
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Get an existing user's data.
     * Access is restricted at the HTTP level to the owner of the account or an ADMIN;
     * no method-level annotation is needed here.
     *
     * @param id the unique identifier of the user to retrieve
     * @return a {@link ResponseEntity} containing the user data {@link UserResponse}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id){
        // Delegate the lookup to the service
        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * Partially updates an existing user's data with the provided values.
     *
     * @param id      the unique identifier of the user to update
     * @param request the fields to update on the user
     * @return a {@link ResponseEntity} containing the updated {@link UserResponse}
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String id, @RequestBody @Valid UserUpdateRequest request){
        // Delegate the partial update to the service
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    /**
     * Deletes an existing user.
     *
     * @param id the unique identifier of the user to delete
     * @return a {@link ResponseEntity} with HTTP 204 and no content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> deleteUser(@PathVariable String id){
        // Delete the user then return an empty 204 response
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates the role of a user. Restricted to admin use.
     *
     * @param id      the unique identifier of the user whose role is being updated
     * @param request the new role to assign to the user
     * @return a {@link ResponseEntity} containing the updated {@link UserResponse}
     */
    @PatchMapping("/admin/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateRole(@PathVariable String id, @RequestBody @Valid UserRoleUpdateRequest request){
        // Only admins reach this point; delegate the role change to the service
        return ResponseEntity.ok(userService.updateRole(id, request));
    }
}

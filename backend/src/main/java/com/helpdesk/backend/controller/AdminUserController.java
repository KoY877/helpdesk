package com.helpdesk.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helpdesk.backend.Data_Transfert_Object.UserResponse;
import com.helpdesk.backend.Data_Transfert_Object.UserRoleUpdateRequest;
import com.helpdesk.backend.service.UserService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@AllArgsConstructor
@Validated
// @PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    /**
     * Updates the role of a user. Restricted to admin access.
     *
     * @param id      the unique identifier of the user whose role is being updated
     * @param request the new role to assign to the user
     * @return a {@link ResponseEntity} containing the updated {@link UserResponse}
     */
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateRole(@PathVariable String id,
                                                    @RequestBody UserRoleUpdateRequest request) {
        // Delegate the role change to the service
        return ResponseEntity.ok(userService.updateRole(id, request));
    }
}

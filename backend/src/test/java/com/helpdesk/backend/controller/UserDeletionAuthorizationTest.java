package com.helpdesk.backend.controller;

import com.helpdesk.backend.model.User;
import com.helpdesk.backend.model.enums.Role;
import com.helpdesk.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDeletionAuthorizationTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean UserService service() { return mock(UserService.class); }
        @Bean UserController controller(UserService service) { return new UserController(service); }
    }

    @Test
    void ownerAndAdminCanDeleteButAnotherUserCannot() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(UserController.class);
            var service = context.getBean(UserService.class);
            User owner = new User(); owner.setId("owner"); owner.setRole(Role.USER);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities()));
            assertEquals(204, controller.deleteUser("owner").getStatusCode().value());
            assertThrows(AccessDeniedException.class, () -> controller.deleteUser("other"));
            verify(service, never()).deleteUser("other");
            owner.setRole(Role.ADMIN);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(owner, null, owner.getAuthorities()));
            assertEquals(204, controller.deleteUser("other").getStatusCode().value());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

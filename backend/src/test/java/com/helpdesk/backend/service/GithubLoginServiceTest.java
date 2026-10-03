package com.helpdesk.backend.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.model.enums.Role;
import com.helpdesk.backend.repository.UserRepository;
import com.helpdesk.backend.security.JwtService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GithubLoginServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final RefreshTokenService refresh = mock(RefreshTokenService.class);
    private final GithubLoginService service = new GithubLoginService(users, passwords, jwt, refresh);

    @Test
    void createsUserWithDefaultRoleAndIssuesTokens() {
        when(users.findByGithubId("42")).thenReturn(Optional.empty());
        when(passwords.encode(anyString())).thenReturn("hash");
        when(users.save(any())).thenAnswer(call -> { User user = call.getArgument(0); user.setId("local"); return user; });
        when(jwt.generateToken(any())).thenReturn("access");
        when(refresh.createRefreshToken(any())).thenReturn("refresh");
        var result = service.login("42", "Alice", "alice@example.com");
        assertEquals("USER", result.role());
        assertEquals("local", result.userId());
        assertEquals("access", result.token());
        verify(users).save(argThat(user -> "42".equals(user.getGithubId()) && user.getRole() == Role.USER));
    }

    @Test
    void existingGithubIdentityKeepsItsLocalAccount() {
        User user = new User(); user.setId("existing"); user.setRole(Role.AGENT);
        when(users.findByGithubId("42")).thenReturn(Optional.of(user));
        assertEquals("existing", service.login("42", "Alice", "changed@example.com").userId());
        verify(users, never()).save(any());
    }

    @Test
    void doesNotTakeOverExistingPasswordAccount() {
        when(users.findByGithubId("42")).thenReturn(Optional.empty());
        when(users.existsByEmail("alice@example.com")).thenReturn(true);
        assertThrows(BadCredentialsException.class, () -> service.login("42", "Alice", "alice@example.com"));
        verifyNoInteractions(jwt, refresh);
    }
}

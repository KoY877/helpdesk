package com.helpdesk.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.helpdesk.backend.dto.AuthResponse;
import com.helpdesk.backend.dto.LoginRequest;
import com.helpdesk.backend.dto.UserCreateRequest;
import com.helpdesk.backend.exception.EmailAlreadyExistsException;
import com.helpdesk.backend.exception.ResourceNotFoundException;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.model.enums.Role;
import com.helpdesk.backend.repository.UserRepository;
import com.helpdesk.backend.security.JwtService;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @InjectMocks private AuthService authService;



    @Test
    void register_withNewEmail_returnsAuthResponse() {
        User user = new User();
        user.setId("u1");
        user.setName("Kodjo");
        user.setEmail("test@test.com");
        user.setPassword("encoded");
        user.setRole(Role.USER);

        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);
        when(jwtService.generateToken(any())).thenReturn("mocked-token");

        UserCreateRequest request = new UserCreateRequest("Kodjo", "test@test.com", "djYn6V&$q!&eTwx4");
        AuthResponse result = authService.register(request);

        assertThat(result.token()).isEqualTo("mocked-token");
        assertThat(result.role()).isEqualTo("USER");
    }

    @Test 
    void register_withExistingEmail_throwsEmailAlreadyExistsException(){
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertThatThrownBy(() -> 
            authService.register(new UserCreateRequest("Kodjo", "test@test.com","zfosdhhr"))
        ).isInstanceOf(EmailAlreadyExistsException.class);
    }
    
    @Test 
    void login_withValidCredentials_returnsAuthResponse() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("encoded");
        user.setRole(Role.USER);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("djYn6V&$q!&eTwx4", "encoded")).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("mocked-token");

        LoginRequest request = new LoginRequest("test@test.com", "djYn6V&$q!&eTwx4");
        AuthResponse result = authService.login(request);

        assertThat(result.token()).isEqualTo("mocked-token");
    }
    
    @Test 
    void login_withUnknownEmail_throwsBadCredentialsException() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> 
            authService.login(new LoginRequest("test@test.com","djYn6V&$q!&eTwx4"))
        ).isInstanceOf(BadCredentialsException.class);
    }
    
    @Test 
    void login_withWrongPassword_throwsBadCredentialsException() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("encoded");

         when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("djYn6V&$q!&eTwx4", "encoded")).thenReturn(false);

        assertThatThrownBy(() ->
            authService.login(new LoginRequest("test@test.com","djYn6V&$q!&eTwx4"))
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_withValidToken_returnsAuthResponseAndRotatesToken() {
        User user = new User();
        user.setId("u1");
        user.setEmail("test@test.com");
        user.setRole(Role.USER);

        when(refreshTokenService.validateRefreshToken("old-refresh-token")).thenReturn("u1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("new-access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("new-refresh-token");

        AuthResponse result = authService.refresh("old-refresh-token");

        assertThat(result.token()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.role()).isEqualTo("USER");
        assertThat(result.userId()).isEqualTo("u1");
        verify(refreshTokenService).revokeToken("old-refresh-token");
    }

    @Test
    void refresh_withInvalidToken_throwsBadCredentialsException() {
        when(refreshTokenService.validateRefreshToken("bad-token"))
            .thenThrow(new BadCredentialsException("Invalid refresh token"));

        assertThatThrownBy(() ->
            authService.refresh("bad-token")
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_withUnknownUser_throwsResourceNotFoundException() {
        when(refreshTokenService.validateRefreshToken("old-refresh-token")).thenReturn("missing-user");
        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            authService.refresh("old-refresh-token")
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void logout_withValidToken_revokesRefreshToken() {
        authService.logout("refresh-token");

        verify(refreshTokenService).revokeToken("refresh-token");
    }

    @Test
    void logout_withUnknownToken_throwsBadCredentialsException() {
        doThrow(new BadCredentialsException("Invalid refresh token"))
            .when(refreshTokenService).revokeToken("unknown-token");

        assertThatThrownBy(() ->
            authService.logout("unknown-token")
        ).isInstanceOf(BadCredentialsException.class);
    }
}

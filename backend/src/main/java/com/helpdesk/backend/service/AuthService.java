package com.helpdesk.backend.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.helpdesk.backend.dto.AuthResponse;
import com.helpdesk.backend.dto.LoginRequest;
import com.helpdesk.backend.dto.UserCreateRequest;
import com.helpdesk.backend.exception.EmailAlreadyExistsException;
import com.helpdesk.backend.exception.ResourceNotFoundException;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.model.enums.Role;
import com.helpdesk.backend.repository.UserRepository;
import com.helpdesk.backend.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Registers a new user and issues an authentication token.
     * The role always defaults to {@link Role#USER} and is never taken from the request.
     *
     * @param request the registration data (name, email, password)
     * @return an {@link AuthResponse} containing the JWT, the role and the user id
     * @throws EmailAlreadyExistsException if the email is already registered
     */
    public AuthResponse register(UserCreateRequest request) {
        // Reject duplicate email addresses
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already in use");
        }

        // Build the new user from the request
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        // Store the password hashed, never in clear text
        user.setPassword(passwordEncoder.encode(request.password()));
        // Force the default role; clients cannot choose it
        user.setRole(Role.USER);
        // Place the user at the end of the ordering sequence
        user.setOrder(userRepository.findMaxOrder() + 1);

        // Persist the user before issuing tokens
        User saved = userRepository.save(user);
        String accessToken = jwtService.generateToken(saved);
        String refreshToken = refreshTokenService.createRefreshToken(saved);

        // Credentials are valid: issue a token
        return new AuthResponse(accessToken, refreshToken, saved.getRole().name(), saved.getId());
    }

    /**
     * Authenticates a user against the stored credentials.
     *
     * @param request the login data (email, password)
     * @return an {@link AuthResponse} containing the JWT, the role and the user id
     * @throws BadCredentialsException if the email is unknown or the password is wrong
     */
    public AuthResponse login(LoginRequest request) {
        // Look up the user by email, using a generic error to avoid leaking which part failed
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Verify the supplied password against the stored hash
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        // Credentials are valid: issue a token
        return new AuthResponse(accessToken, refreshToken, user.getRole().name(), user.getId());
    }

    /**
     * Issues a new access token and rotates the refresh token.
     *
     * @param refreshToken the raw refresh token presented by the client
     * @return an {@link AuthResponse} containing the new JWT, refresh token, role and user id
     * @throws BadCredentialsException if the refresh token is unknown, revoked or expired
     * @throws ResourceNotFoundException if the user the token was issued to no longer exists
     */
    public AuthResponse refresh(String refreshToken) {
        // Validate the token and resolve the user it was issued to
        String userId = refreshTokenService.validateRefreshToken(refreshToken);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Revoke the old refresh token and issue a new one (rotation prevents reuse)
        refreshTokenService.revokeToken(refreshToken);
        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = refreshTokenService.createRefreshToken(user);

        return new AuthResponse(newAccessToken, newRefreshToken, user.getRole().name(), user.getId());
    }

    /**
     * Logs a user out by revoking their refresh token so it can no longer be used to obtain new access tokens.
     *
     * @param refreshToken the raw refresh token to revoke
     * @throws BadCredentialsException if the refresh token is unknown
     */
    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }
}
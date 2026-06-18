package com.helpdesk.backend.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.helpdesk.backend.Data_Transfert_Object.AuthResponse;
import com.helpdesk.backend.Data_Transfert_Object.LoginRequest;
import com.helpdesk.backend.Data_Transfert_Object.UserCreateRequest;
import com.helpdesk.backend.exception.EmailAlreadyExistsException;
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

        // Persist the user and return a freshly generated token
        User saved = userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(saved), saved.getRole().name(), saved.getId());
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

        // Credentials are valid: issue a token
        return new AuthResponse(jwtService.generateToken(user), user.getRole().name(), user.getId());
    }
}
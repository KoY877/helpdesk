package com.helpdesk.backend.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.helpdesk.backend.model.RefreshToken;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.repository.RefreshTokenRepository;
import com.helpdesk.backend.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    /**
     * Issues and persists a new refresh token for the given user.
     *
     * @param user the user to issue the refresh token for
     * @return the raw refresh token string to hand back to the client
     */
    public String createRefreshToken(User user) {
        // The token itself is just an opaque random identifier
        String token = jwtService.generateRefreshToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        // Expire it after the configured refresh-token lifetime
        refreshToken.setExpiryDate(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration)));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    /**
     * Validates a refresh token and returns the id of the user it belongs to.
     *
     * @param token the raw refresh token to validate
     * @return the id of the user the token was issued to
     * @throws BadCredentialsException if the token is unknown, revoked or expired
     */
    public String validateRefreshToken(String token) {
        // Use a generic error so callers can't distinguish unknown vs. expired vs. revoked
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        return refreshToken.getUser().getId();
    }

    /**
     * Revokes a refresh token so it can no longer be used to obtain new access tokens.
     *
     * @param token the raw refresh token to revoke
     * @throws BadCredentialsException if the token is unknown
     */
    public void revokeToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
}

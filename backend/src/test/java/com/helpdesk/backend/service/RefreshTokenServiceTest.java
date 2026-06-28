package com.helpdesk.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import com.helpdesk.backend.model.RefreshToken;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.repository.RefreshTokenRepository;
import com.helpdesk.backend.security.JwtService;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @InjectMocks private RefreshTokenService refreshTokenService;

    @Test
    void createRefreshToken_savesAndReturnsToken() {
        User user = new User();
        user.setId("u1");

        when(jwtService.generateRefreshToken()).thenReturn("generated-token");

        String result = refreshTokenService.createRefreshToken(user);

        assertThat(result).isEqualTo("generated-token");
        // Verify the persisted entity carries the generated token, the owning user and starts unrevoked
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("generated-token");
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().isRevoked()).isFalse();
    }

    @Test
    void validateRefreshToken_withValidToken_returnsUserId() {
        User user = new User();
        user.setId("u1");

        RefreshToken token = new RefreshToken();
        token.setToken("valid-token");
        token.setUser(user);
        token.setRevoked(false);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        String userId = refreshTokenService.validateRefreshToken("valid-token");

        assertThat(userId).isEqualTo("u1");
    }

    @Test
    void validateRefreshToken_withUnknownToken_throwsBadCredentialsException() {
        when(refreshTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            refreshTokenService.validateRefreshToken("unknown-token")
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void validateRefreshToken_withExpiredToken_throwsBadCredentialsException() {
        RefreshToken token = new RefreshToken();
        token.setToken("expired-token");
        token.setUser(new User());
        token.setRevoked(false);
        token.setExpiryDate(LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() ->
            refreshTokenService.validateRefreshToken("expired-token")
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void validateRefreshToken_withRevokedToken_throwsBadCredentialsException() {
        RefreshToken token = new RefreshToken();
        token.setToken("revoked-token");
        token.setUser(new User());
        token.setRevoked(true);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() ->
            refreshTokenService.validateRefreshToken("revoked-token")
        ).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void revokeToken_setsRevokedTrue() {
        RefreshToken token = new RefreshToken();
        token.setToken("token-to-revoke");
        token.setRevoked(false);

        when(refreshTokenRepository.findByToken("token-to-revoke")).thenReturn(Optional.of(token));

        refreshTokenService.revokeToken("token-to-revoke");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void revokeToken_withUnknownToken_throwsBadCredentialsException() {
        when(refreshTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            refreshTokenService.revokeToken("unknown-token")
        ).isInstanceOf(BadCredentialsException.class);
    }
}

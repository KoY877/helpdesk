package com.helpdesk.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helpdesk.backend.dto.AuthResponse;
import com.helpdesk.backend.dto.LoginRequest;
import com.helpdesk.backend.dto.RefreshTokenRequest;
import com.helpdesk.backend.dto.UserCreateRequest;
import com.helpdesk.backend.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.helpdesk.backend.security.OAuthHandoff oauthHandoff;

    /** Code temporaire envoyé par Angular après le retour de GitHub. */
    public record OAuthExchangeRequest(String code) {}

    /**
     * Échange le code OAuth temporaire contre les jetons du compte local.
     * @param body code reçu dans le fragment de l'URL de retour
     * @param request requête portant le cookie de la session OAuth
     * @return jetons et informations du compte, avec mise en cache interdite
     */
    @PostMapping("/oauth/exchange")
    public ResponseEntity<AuthResponse> exchange(@RequestBody OAuthExchangeRequest body,
            jakarta.servlet.http.HttpServletRequest request) {
        // getSession(false) exige une session existante sans en créer une nouvelle.
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(oauthHandoff.exchange(request.getSession(false), body.code()));
    }

    /**
     * Registers a new user account.
     *
     * @param request the registration data (name, email, password)
     * @return HTTP 201 with an {@link AuthResponse} containing the JWT and user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid UserCreateRequest request) {
        // Delegate to the service and return 201 Created on success
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Authenticates a user and returns a JWT.
     *
     * @param request the login data (email, password)
     * @return HTTP 200 with an {@link AuthResponse} containing the JWT and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        // Delegate to the service and return 200 OK with the token
        return ResponseEntity.ok(authService.login(request));
    }

     /**
     * Authenticates a user and returns a JWT.
     *
     * @param request the login data (email, password)
     * @return HTTP 200 with an {@link AuthResponse} containing the JWT and user info
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    /**
     * Logs a user out by revoking their refresh token.
     *
     * @param request the refresh token to revoke
     * @return HTTP 204 with no content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

}

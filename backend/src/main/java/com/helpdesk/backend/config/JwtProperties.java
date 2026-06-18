package com.helpdesk.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Type-safe holder for the {@code jwt.*} configuration properties:
 * the signing secret and the access/refresh token expirations.
 */
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    // Base64-encoded secret used to sign the tokens
    private String secret;
    // Access-token settings (short-lived)
    private AccessToken accessToken = new AccessToken();
    // Refresh-token settings (long-lived)
    private RefreshToken refreshToken = new RefreshToken();

    /** Access-token configuration. */
    @Data
    public static class AccessToken {
        private Long expiration = 36000000L; // 15 minutes by default

    }

    /** Refresh-token configuration. */
      @Data
    public static class RefreshToken {
        private Long expiration = 604800000L; // 7 days by default

    }
}

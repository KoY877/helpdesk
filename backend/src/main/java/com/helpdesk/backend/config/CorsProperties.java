package com.helpdesk.backend.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Type-safe holder for the {@code cors.*} configuration properties.
 * Defaults keep the local front-end origin working when nothing is overridden.
 */
@ConfigurationProperties(prefix = "cors")
@Data
public class CorsProperties {

    // Origins allowed to call the API (the front-end origin)
    private List<String> allowedOrigins = List.of("http://localhost:4200");
    // HTTP methods the front-end is allowed to use
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    // Request headers the front-end is allowed to send
    private List<String> allowedHeaders = List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With");
    // Whether credentials (cookies/Authorization) may be sent with CORS requests
    private boolean allowCredentials = true;
    // How long (seconds) browsers may cache the CORS preflight response
    private long maxAge = 3600;
}

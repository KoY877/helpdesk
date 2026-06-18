package com.helpdesk.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.helpdesk.backend.security.JwtAuthFilter;
import com.helpdesk.backend.security.JwtService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JwtService jwtService;
    private final CorsProperties corsProperties;

    /**
     * Defines the CORS policy applied to every endpoint, driven by the
     * {@code cors.*} configuration properties.
     *
     * @return the configured {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Build the policy from the externalized cors.* properties
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(corsProperties.getAllowedMethods());
        config.setAllowedHeaders(corsProperties.getAllowedHeaders());
        config.setAllowCredentials(corsProperties.isAllowCredentials());
        config.setMaxAge(corsProperties.getMaxAge());
        // Apply this configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Builds the main security filter chain: stateless JWT authentication,
     * CORS enabled, CSRF disabled and per-endpoint authorization rules.
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the built {@link SecurityFilterChain}
     * @throws Exception if the configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Enable CORS using the source defined above
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF protection is unnecessary for a stateless token-based API
            .csrf(AbstractHttpConfigurer::disable)
            // Never create an HTTP session; every request is authenticated by its token
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Authentication endpoints are public
                .requestMatchers("/api/auth/**").permitAll()
                // Listing all users is reserved to agents and admins
                .requestMatchers(HttpMethod.GET, "/api/users/all").hasAnyRole("AGENT", "ADMIN")
                // Reading a single user is allowed to its owner or an admin
                .requestMatchers(HttpMethod.GET, "/api/users/{id}").access(ownerOrAdmin())
                // Everything else simply requires authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                // Return a JSON 401 body instead of the default HTML error page
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Unauthorized\"}");
                })
            )
            // Run the JWT filter before the standard username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Authorization rule granting access when the caller is an admin or the
     * owner of the requested user resource.
     *
     * @return an {@link AuthorizationManager} enforcing the owner-or-admin rule
     */
    private AuthorizationManager<RequestAuthorizationContext> ownerOrAdmin() {
        return (principal, ctx) -> {
            // Admins are always allowed
            boolean isAdmin = principal.get().getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) return new AuthorizationDecision(true);

            // A non-admin must present a Bearer token to be checked for ownership
            String header = ctx.getRequest().getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer "))
                return new AuthorizationDecision(false);

            // Grant access only when the token's user id matches the requested id
            String tokenUserId = jwtService.extractUserId(header.substring(7));
            String pathId = ctx.getVariables().get("id");
            return new AuthorizationDecision(tokenUserId != null && tokenUserId.equals(pathId));
        };
    }

    /**
     * Provides the password encoder used to hash and verify passwords.
     *
     * @return a BCrypt {@link PasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt automatically handles salting and a configurable work factor
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the Spring {@link AuthenticationManager} as a bean.
     *
     * @param config the authentication configuration provided by Spring
     * @return the application's {@link AuthenticationManager}
     * @throws Exception if the manager cannot be obtained
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // Delegate to Spring's auto-configured authentication manager
        return config.getAuthenticationManager();
    }
}

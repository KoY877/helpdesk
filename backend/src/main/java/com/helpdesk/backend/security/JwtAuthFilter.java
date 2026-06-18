package com.helpdesk.backend.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Runs once per request: reads the Bearer token, validates it and, if valid,
     * populates the {@link SecurityContextHolder} with the authenticated user.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the remaining filter chain to continue with
     * @throws ServletException if the filter chain fails
     * @throws IOException      if an I/O error occurs while processing the request
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Read the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // Without a Bearer token there is nothing to authenticate; continue the chain
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Strip the "Bearer " prefix to get the raw token
        final String jwt = authHeader.substring(7);

        try {
            // Extract the email carried by the token
            final String email = jwtService.extractEmail(jwt);

            // Only authenticate if we have an email and no authentication is set yet
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load the user behind the token
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                // If the token genuinely belongs to that user and is not expired, authenticate
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ignored) {
            // invalid/expired/tampered token — treat as unauthenticated
        }

        // Always continue the chain, authenticated or not
        filterChain.doFilter(request, response);
    }
}
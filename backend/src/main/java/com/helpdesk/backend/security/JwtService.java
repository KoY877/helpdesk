package com.helpdesk.backend.security;

import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.helpdesk.backend.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token.expiration}")
    private long expiration;
    
    /**
     * Builds the HMAC signing key from the configured Base64 secret.
     *
     * @return the {@link SecretKey} used to sign and verify tokens
     */
    private SecretKey getSigninKey(){
        // Decode the Base64-encoded secret into raw bytes
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        // Derive an HMAC-SHA key from those bytes
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed JWT for the given user.
     *
     * @param user the user to issue the token for
     * @return a compact, signed JWT carrying the email, role and user id
     */
    public String generateToken(User user){
        // Build the token with the standard and custom claims, then sign it
        return Jwts.builder()
                    .subject(user.getEmail())
                    .claim("role", user.getRole().name())
                    .claim("userId", user.getId())
                    .issuedAt(new Date())
                    // Expire the token after the configured access-token lifetime
                    .expiration(new Date(System.currentTimeMillis() + expiration))
                    .signWith(getSigninKey())
                    .compact();
    }

   /**
    * Extracts the email (subject) from a token.
    *
    * @param token the JWT to read
    * @return the email stored in the subject claim
    */
   public String extractEmail(String token){
    // The email is stored as the token's subject
    return extractClaim(token, Claims::getSubject);
   }

   /**
    * Extracts the user id from a token.
    *
    * @param token the JWT to read
    * @return the user id stored in the custom claim
    */
   public String extractUserId(String token){
    // Read the custom "userId" claim
    return extractClaim(token, claims -> claims.get("userId", String.class));
   }

   /**
    * Checks that a token belongs to the given user and is not expired.
    *
    * @param token       the JWT to validate
    * @param userDetails the user the token is expected to belong to
    * @return true if the token is valid for the user, false otherwise
    */
   public boolean isTokenValid(String token, UserDetails userDetails){
        // The token is valid if its subject matches the user and it has not expired
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);

   }

   /**
    * Tells whether a token's expiration date has passed.
    *
    * @param token the JWT to check
    * @return true if the token is expired, false otherwise
    */
   public boolean isTokenExpired(String token){
        // Compare the expiration claim with the current time
        return extractClaim(token, Claims::getExpiration).before(new Date());
   }

   /**
    * Parses, verifies and extracts a single claim from a token.
    *
    * @param token    the JWT to parse
    * @param resolver the function selecting the claim to return
    * @param <T>      the type of the resolved claim
    * @return the value produced by the resolver
    */
   public <T> T extractClaim (String token, Function<Claims, T> resolver) {
        // Verify the signature, parse the payload, then apply the resolver to the claims
        return resolver.apply(
            Jwts.parser()
                .verifyWith(getSigninKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
        );
   }
}

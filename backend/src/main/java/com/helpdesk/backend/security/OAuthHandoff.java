package com.helpdesk.backend.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.BadCredentialsException;
import com.helpdesk.backend.dto.AuthResponse;
import jakarta.servlet.http.HttpSession;

/** Transmet les JWT à Angular via un code temporaire lié à la session du navigateur. */
@Component
public class OAuthHandoff {
    // Nom unique de l'attribut contenant les données de connexion dans la session.
    private static final String KEY = OAuthHandoff.class.getName();
    private final SecureRandom random = new SecureRandom();
    // Données conservées côté serveur jusqu'à l'échange : code, expiration et JWT.
    private record Pending(String code, Instant expires, AuthResponse response) {}

    /**
     * Prépare un échange valable deux minutes sans placer les JWT dans l'URL.
     * @param session session du navigateur ayant terminé l'authentification GitHub
     * @param response jetons et informations du compte local
     * @return code aléatoire à transmettre au frontend
     */
    public String store(HttpSession session, AuthResponse response) {
        // Génère 256 bits aléatoires avec un générateur adapté aux secrets.
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        // Encode le code dans un format compatible avec une URL.
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(KEY, new Pending(code, Instant.now().plusSeconds(120), response));
        return code;
    }

    /**
     * Valide le code et restitue les jetons une seule fois.
     * @param session session existante retrouvée grâce au cookie du navigateur
     * @param code code reçu par Angular après la redirection
     * @return informations de connexion nécessaires pour accéder au dashboard
     * @throws BadCredentialsException si la session, le code ou sa validité sont incorrects
     */
    public AuthResponse exchange(HttpSession session, String code) {
        if (session == null || code == null) throw new BadCredentialsException("Invalid OAuth exchange");
        // Sérialise les échanges concurrents portant sur le même objet session.
        synchronized (session) {
            Pending pending = (Pending) session.getAttribute(KEY);
            // Vérifie la présence des données, la correspondance du code et son expiration.
            if (pending == null || !pending.code().equals(code) || !Instant.now().isBefore(pending.expires())) {
                throw new BadCredentialsException("Invalid OAuth exchange");
            }
            // Consomme le code et ferme la session temporaire ; la suite utilise les JWT.
            session.removeAttribute(KEY);
            session.invalidate();
            return pending.response();
        }
    }
}

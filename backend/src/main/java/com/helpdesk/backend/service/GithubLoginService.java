package com.helpdesk.backend.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.helpdesk.backend.model.User;
import com.helpdesk.backend.model.enums.Role;
import com.helpdesk.backend.repository.UserRepository;
import com.helpdesk.backend.dto.AuthResponse;
import com.helpdesk.backend.security.JwtService;
import lombok.RequiredArgsConstructor;

/** Associe une identité GitHub à un compte local et émet les jetons de l'application. */
@Service
@RequiredArgsConstructor
public class GithubLoginService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;

    /**
     * Connecte un compte GitHub connu ou crée un utilisateur avec le rôle USER.
     * @param githubId identifiant stable fourni par GitHub
     * @param name nom public ou pseudonyme GitHub
     * @param verifiedEmail adresse vérifiée nécessaire à la création du compte
     * @return JWT d'accès, jeton de renouvellement, rôle et identifiant local
     * @throws BadCredentialsException si l'adresse manque ou appartient déjà à un autre compte
     */
    @Transactional
    public AuthResponse login(String githubId, String name, String verifiedEmail) {
        // Un compte déjà lié est retrouvé par son identifiant GitHub, même si son e-mail change.
        User user = users.findByGithubId(githubId).orElseGet(() -> {
            if (verifiedEmail == null || verifiedEmail.isBlank()) {
                throw new BadCredentialsException("verified_email_required");
            }
            // Évite de fusionner automatiquement un compte à mot de passe sur la seule base de l'e-mail.
            if (users.existsByEmail(verifiedEmail)) {
                throw new BadCredentialsException("account_link_required");
            }
            User created = new User();
            created.setGithubId(githubId);
            created.setEmail(verifiedEmail);
            created.setName(name);
            // Stocke un mot de passe aléatoire haché, inconnu de l'utilisateur GitHub.
            created.setPassword(passwords.encode(UUID.randomUUID().toString()));
            // Un nouvel utilisateur ne peut pas choisir un rôle privilégié via son profil GitHub.
            created.setRole(Role.USER);
            created.setOrder(users.findMaxOrder() + 1);
            return users.save(created);
        });
        // Émet les mêmes types de jetons que la connexion classique par mot de passe.
        return new AuthResponse(jwt.generateToken(user), refreshTokens.createRefreshToken(user),
                user.getRole().name(), user.getId());
    }
}

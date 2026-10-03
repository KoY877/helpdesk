package com.helpdesk.backend.security;

import java.io.IOException;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.client.RestClient;
import com.helpdesk.backend.service.GithubLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** Relie l'authentification GitHub réussie à la connexion locale puis au frontend. */
@Component
@RequiredArgsConstructor
public class GithubSuccessHandler implements AuthenticationSuccessHandler {
    private final OAuth2AuthorizedClientService clients;
    private final GithubLoginService login;
    private final OAuthHandoff handoff;
    // Adresse configurée côté serveur : destination autorisée de la redirection.
    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;
    /** Champs utiles d'une adresse e-mail renvoyée par l'API GitHub. */
    public record Email(String email, boolean primary, boolean verified) {}

    /**
     * Appelée par Spring Security après validation du retour OAuth de GitHub.
     * Récupère une adresse vérifiée, connecte le compte local et redirige vers Angular.
     * @param request requête du navigateur contenant la session OAuth
     * @param response réponse utilisée pour la redirection
     * @param authentication identité GitHub authentifiée par Spring Security
     * @throws IOException si la réponse de redirection ne peut pas être écrite
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) authentication;
        try {
            // Retrouve le jeton GitHub obtenu par Spring, distinct des JWT de l'application.
            var client = clients.loadAuthorizedClient(oauth.getAuthorizedClientRegistrationId(), oauth.getName());
            // Le scope user:email permet également de récupérer les adresses privées.
            Email[] emails = RestClient.create().get().uri("https://api.github.com/user/emails")
                    .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
                    .retrieve().body(Email[].class);
            // Ne retient que les adresses vérifiées, en donnant priorité à l'adresse principale.
            String email = emails == null ? null : Arrays.stream(emails)
                    .filter(Email::verified).sorted(java.util.Comparator.comparing(Email::primary).reversed())
                    .map(Email::email).findFirst().orElse(null);
            // L'identifiant GitHub stable sert à retrouver le compte lors des connexions suivantes.
            Object id = oauth.getPrincipal().getAttribute("id");
            if (id == null) throw new BadCredentialsException("github_login_failed");
            // Utilise le pseudonyme lorsque le nom public n'est pas renseigné.
            String name = oauth.getPrincipal().getAttribute("name");
            if (name == null || name.isBlank()) name = oauth.getPrincipal().getAttribute("login");
            // Crée ou retrouve le compte local, puis prépare un échange temporaire des JWT.
            var tokens = login.login(id.toString(), name, email);
            String code = handoff.store(request.getSession(), tokens);
            response.setHeader("Cache-Control", "no-store");
            // Angular lit le fragment et échange le code avant de naviguer vers le dashboard.
            response.sendRedirect(frontendUrl + "/oauth/callback#code=" + code);
        } catch (RuntimeException exception) {
            // Transmet un identifiant d'erreur limité, sans détails internes ni secrets.
            String error = exception instanceof BadCredentialsException
                    && "account_link_required".equals(exception.getMessage())
                    ? "account_link_required" : "github_login_failed";
            response.sendRedirect(frontendUrl + "/oauth/callback#error=" + error);
        } finally {
            // Supprime le jeton GitHub conservé localement ; cela ne le révoque pas chez GitHub.
            clients.removeAuthorizedClient(oauth.getAuthorizedClientRegistrationId(), oauth.getName());
        }
    }
}
